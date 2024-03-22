

function generate_shorturl() {

    var longURL = encodeURIComponent(document.getElementById("long-url").value);
    var shortUrl = encodeURIComponent(document.getElementById("short-url").value);

    var http = new XMLHttpRequest();
    http.open("POST", "/api/shorten", true);
    http.setRequestHeader("Content-type", "application/json");
    var params = { "url": longURL, "surl": shortUrl };
    http.send(JSON.stringify(params));
    http.onload = function () {
        var api_reply = JSON.parse(http.responseText);
        if (api_reply['status'] == "OK") {
            if (api_reply['content']['isEnabled']) {
                document.getElementById("result-error").style.display = "none";
            }
            else {
                document.getElementById("result-error").innerHTML = '<div class="alert alert-warning" role="alert"><strong>Notice:</strong>ShortURL has been generated, however pending admin review.</div>';
                document.getElementById("result-error").style.display = "";
            }
            let link = location.href + api_reply['content']['surl'];
            document.getElementById("success-alert").innerHTML = "Short URL Generated.";
            document.getElementById("short_url").value = link
            document.getElementById("delete_link").value = location.href + "delete/" + api_reply['content']['surl'] + "/" + api_reply['content']['deleteKey'];
            document.getElementById("qr-code").src = "/qr/" + btoa(link);
            document.getElementById("result-box").style.display = "";

        }
        else if (api_reply['status'] == "FAIL") {
            document.getElementById("result-error").innerHTML = '<div class="alert alert-danger" role="alert"><strong>ERROR:</strong> ' + api_reply['error'] + '</div>';
            document.getElementById("result-error").style.display = "";
            document.getElementById("result-box").style.display = "none";

            if (api_reply['content']['surl'] != undefined) {
                let link = location.href + api_reply['content']['surl'];
                document.getElementById("success-alert").style.display = "none";
                document.getElementById("short_url").value = link
                document.getElementById("delete_link").value = "(only visible to creator)";
                document.getElementById("qr-code").src = "/qr/" + btoa(link);
                document.getElementById("result-box").style.display = "";
            }
        }
    }

    document.getElementById('slider').classList.remove('closed');
    return false;
}



function admin_get_logs(surl) {
    var divBox = document.getElementById('logsTable');

    var http = new XMLHttpRequest();
    http.open("GET", "/admin/logs/" + surl, true);
    http.send();
    http.onload = function () {
        divBox.innerHTML = http.responseText;
    }
    divBox.classList.remove('closed');
    return false;
}



function copy_link(el) {
    var copyText = el.value;
    copyTextToClipboard(copyText);
    button_obj.innerHTML = "Copied";
    return false;
}



// Fallback function to copy text to clipboard
function fallbackCopyTextToClipboard(text) {
    var textArea = document.createElement("textarea");
    textArea.value = text;

    // Avoid scrolling to bottom
    textArea.style.top = "0";
    textArea.style.left = "0";
    textArea.style.position = "fixed";

    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    try {
        var successful = document.execCommand('copy');
        var msg = successful ? 'successful' : 'unsuccessful';
        console.log('Fallback: Copying text command was ' + msg);
    } catch (err) {
        console.error('Fallback: Oops, unable to copy', err);
    }

    document.body.removeChild(textArea);
}



// Main function to copy text to clipboard
function copyTextToClipboard(text) {
    if (!navigator.clipboard) {
        fallbackCopyTextToClipboard(text);
        return;
    }
    navigator.clipboard.writeText(text).then(
        function () {
            console.log('Async: Copying to clipboard was successful!');
        }, function (err) {
            console.error('Async: Could not copy text: ', err);
        }
    );
}



