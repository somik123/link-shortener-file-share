package com.kfels.shorturl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.kfels.shorturl.entity.Shorturl;
import com.kfels.shorturl.repo.ShorturlRepo;
import com.kfels.shorturl.utils.CommonUtils;

class ShorturlServiceImplTests {

    private ShorturlRepo repo;
    private ShorturlServiceImpl service;

    @BeforeEach
    void setUp() {
        repo = mock(ShorturlRepo.class);
        service = new ShorturlServiceImpl(repo);
    }

    @Test
    void generatesShorturlWithProvidedUniqueAlias() {
        when(repo.findBySurlHash(CommonUtils.getHash("known-alias"))).thenReturn(List.of());

        Shorturl generated = service.generateShorturl("https://example.com/path", "192.0.2.1", "known-alias");

        assertEquals("known-alias", generated.getSurl());
        assertEquals("https://example.com/path", generated.getLongUrl());
        assertEquals("192.0.2.1", generated.getCreatorIp());
        verify(repo).save(generated);
    }

    @Test
    void generatesRandomShorturlWhenProvidedAliasIsBlank() {
        when(repo.findBySurlHash(any())).thenReturn(List.of());

        Shorturl generated = service.generateShorturl("https://example.com", "192.0.2.2", "");

        assertEquals(CommonUtils.getShortUrlLength(), generated.getSurl().length());
        verify(repo).save(generated);
    }

    @Test
    void twoArgumentOverloadSavesGeneratedShorturl() {
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class, CALLS_REAL_METHODS)) {
            commonUtils.when(() -> CommonUtils.generateStringForShorturl(null)).thenReturn("generated-alias");
            when(repo.findBySurlHash(CommonUtils.getHash("generated-alias"))).thenReturn(List.of());

            Shorturl generated = service.generateShorturl("https://example.com", "192.0.2.21");

            assertEquals("generated-alias", generated.getSurl());
            verify(repo).save(generated);
            commonUtils.verify(() -> CommonUtils.generateStringForShorturl(null),
                    org.mockito.Mockito.times(2));
        }
    }

    @Test
    void retriesRandomAliasWhenRequestedAliasIsAlreadyTaken() {
        Shorturl existing = shorturl("taken-alias", true);
        when(repo.findBySurlHash(CommonUtils.getHash("taken-alias"))).thenReturn(List.of(existing));
        when(repo.findBySurlHash(CommonUtils.getHash("replacement"))).thenReturn(List.of());

        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class, CALLS_REAL_METHODS)) {
            commonUtils.when(() -> CommonUtils.generateStringForShorturl(null)).thenReturn("replacement");

            Shorturl generated = service.generateShorturl(
                    "https://example.com/new", "192.0.2.22", "taken-alias");

            assertEquals("replacement", generated.getSurl());
            verify(repo).save(generated);
            commonUtils.verify(() -> CommonUtils.generateStringForShorturl(null),
                    org.mockito.Mockito.times(2));
        }
    }

    @Test
    void doesNotGenerateShorturlWithoutCreatorIp() {
        assertNull(service.generateShorturl("https://example.com", null, "known-alias"));
        assertNull(service.generateShorturl("https://example.com", "", "known-alias"));
        verifyNoInteractions(repo);
    }

    @Test
    void returnsAndPersistsAccessForEnabledShorturl() {
        Shorturl shorturl = shorturl("alias", true);
        when(repo.findBySurlHash(CommonUtils.getHash("alias"))).thenReturn(List.of(shorturl));

        assertEquals("https://example.com/destination", service.accessShorturl("alias", "192.0.2.4", "browser"));

        assertEquals(1, shorturl.getHits());
        assertEquals(1, shorturl.getLogs().size());
        assertEquals("192.0.2.4", shorturl.getLogs().get(0).getHitIp());
        assertEquals("browser", shorturl.getLogs().get(0).getBrowserHeaders());
        assertTrue(shorturl.getLastHit() != null);
        verify(repo).save(shorturl);
    }

    @Test
    void rejectsAccessWhenShorturlOrRequestDetailsAreMissing() {
        when(repo.findBySurlHash(any())).thenReturn(List.of());
        assertNull(service.accessShorturl("missing", "192.0.2.5", "browser"));
        verify(repo, never()).save(any());

        Shorturl shorturl = shorturl("alias", true);
        when(repo.findBySurlHash(CommonUtils.getHash("alias"))).thenReturn(List.of(shorturl));
        assertNull(service.accessShorturl("alias", null, "browser"));
        assertNull(service.accessShorturl("alias", "", "browser"));
        assertNull(service.accessShorturl("alias", "192.0.2.5", null));
        assertNull(service.accessShorturl("alias", "192.0.2.5", ""));
        verify(repo, never()).save(shorturl);
    }

    @Test
    void disabledShorturlReturnsNoDestinationButIsPersisted() {
        Shorturl shorturl = shorturl("alias", false);
        when(repo.findBySurlHash(CommonUtils.getHash("alias"))).thenReturn(List.of(shorturl));

        assertNull(service.accessShorturl("alias", "192.0.2.6", "browser"));
        assertEquals(0, shorturl.getHits());
        verify(repo).save(shorturl);
    }

    @Test
    void deletesWithMatchingDeleteKeyOnly() {
        Shorturl shorturl = shorturl("alias", true);
        shorturl.setDeleteKey("secret");
        when(repo.findBySurlHash(CommonUtils.getHash("alias"))).thenReturn(List.of(shorturl));

        assertTrue(service.deleteShorturl("alias", "secret"));
        verify(repo).delete(shorturl);
    }

    @Test
    void rejectsDeleteWhenShorturlOrKeyIsMissingOrIncorrect() {
        when(repo.findBySurlHash(any())).thenReturn(List.of());
        assertFalse(service.deleteShorturl("missing", "secret"));

        Shorturl shorturl = shorturl("alias", true);
        shorturl.setDeleteKey("secret");
        when(repo.findBySurlHash(CommonUtils.getHash("alias"))).thenReturn(List.of(shorturl));
        assertFalse(service.deleteShorturl("alias", null));
        assertFalse(service.deleteShorturl("alias", ""));
        assertFalse(service.deleteShorturl("alias", "wrong"));
        verify(repo, never()).delete(any());
    }

    @Test
    void findsShorturlByAliasAndHandlesMissingResults() {
        Shorturl expected = shorturl("alias", true);
        when(repo.findBySurlHash(CommonUtils.getHash("alias"))).thenReturn(List.of(expected));

        assertSame(expected, service.getShorturlDetails("alias"));
        assertNull(service.getShorturlDetails(null));
        assertNull(service.getShorturlDetails(""));

        when(repo.findBySurlHash(CommonUtils.getHash("absent"))).thenReturn(null);
        assertNull(service.getShorturlDetails("absent"));
        when(repo.findBySurlHash(CommonUtils.getHash("empty"))).thenReturn(List.of());
        assertNull(service.getShorturlDetails("empty"));
    }

    @Test
    void returnsShorturlsNewestFirst() {
        Shorturl older = shorturl("older", true);
        older.setCreated(LocalDateTime.of(2024, 1, 1, 0, 0));
        Shorturl newer = shorturl("newer", true);
        newer.setCreated(LocalDateTime.of(2025, 1, 1, 0, 0));
        List<Shorturl> rows = new ArrayList<>(List.of(older, newer));
        when(repo.findAllCustom()).thenReturn(rows);

        assertEquals(List.of(newer, older), service.getAllShorturls());
    }

    @Test
    void enablesAndDisablesExistingShorturl() {
        Shorturl shorturl = shorturl("alias", false);
        when(repo.findBySurlHash(CommonUtils.getHash("alias"))).thenReturn(List.of(shorturl));

        assertTrue(service.enableShorturl("alias"));
        assertTrue(shorturl.isEnabled());
        verify(repo).save(shorturl);

        assertTrue(service.disableShorturl("alias"));
        assertFalse(shorturl.isEnabled());
        verify(repo, org.mockito.Mockito.times(2)).save(shorturl);
    }

    @Test
    void missingShorturlAdminMutationsRetainTrueResultWithoutRepositoryWrites() {
        when(repo.findBySurlHash(any())).thenReturn(List.of());

        assertTrue(service.enableShorturl("missing"));
        assertTrue(service.disableShorturl("missing"));
        assertTrue(service.deleteShorturl("missing"));
        verify(repo, never()).save(any());
        verify(repo, never()).delete(any());
    }

    @Test
    void findsShorturlByLongUrlHashAndHandlesMissingResults() {
        Shorturl expected = shorturl("alias", true);
        when(repo.findByLongUrlHash(CommonUtils.getHash("https://example.com")))
                .thenReturn(List.of(expected));

        assertSame(expected, service.getShorturlByLongurl("https://example.com"));
        assertNull(service.getShorturlByLongurl(null));
        assertNull(service.getShorturlByLongurl(""));
        when(repo.findByLongUrlHash(CommonUtils.getHash("https://absent.example"))).thenReturn(List.of());
        assertNull(service.getShorturlByLongurl("https://absent.example"));
        when(repo.findByLongUrlHash(CommonUtils.getHash("https://null.example"))).thenReturn(null);
        assertNull(service.getShorturlByLongurl("https://null.example"));
    }

    @Test
    void reportsAliasUniquenessBasedOnLookup() {
        when(repo.findBySurlHash(CommonUtils.getHash("unused"))).thenReturn(List.of());
        assertTrue(service.isSurlUnique("unused"));

        Shorturl existing = shorturl("used", true);
        when(repo.findBySurlHash(CommonUtils.getHash("used"))).thenReturn(List.of(existing));
        assertFalse(service.isSurlUnique("used"));
    }

    @Test
    void savesNonNullShorturlAndIgnoresNull() {
        Shorturl shorturl = shorturl("alias", true);

        assertSame(shorturl, service.save(shorturl));
        assertNull(service.save(null));

        ArgumentCaptor<Shorturl> saved = ArgumentCaptor.forClass(Shorturl.class);
        verify(repo).save(saved.capture());
        assertSame(shorturl, saved.getValue());
    }

    private Shorturl shorturl(String alias, boolean enabled) {
        Shorturl shorturl = new Shorturl("https://example.com/destination", "192.0.2.10", alias);
        shorturl.setEnabled(enabled);
        return shorturl;
    }
}
