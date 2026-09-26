package com.kfels.shorturl.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class DnsBlockListTests {

    @Test
    void malformedUrlCannotMatchBlockedDomain() {
        assertFalse(DnsBlockList.checkBlockList("not a valid url"));
    }
}
