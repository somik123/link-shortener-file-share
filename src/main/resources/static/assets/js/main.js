

function generate_shorturl() {

    var submitBtn = document.getElementById("submit_btn");

    var longURL = document.getElementById("long-url").value;
    var shortUrl = document.getElementById("short-url").value;

    submit_btn.innerHTML = "Processing...";

    var http = new XMLHttpRequest();
    http.open("POST", "/api/shorten", true);
    http.setRequestHeader("Content-type", "application/json");
    var params = { "url": longURL, "surl": shortUrl };
    http.send(JSON.stringify(params));
    http.onload = function () {
        document.getElementById('slider').classList.remove('closed');
        submit_btn.innerHTML = "Shorten";
        var api_reply = JSON.parse(http.responseText);
        console.log(api_reply);
        if (api_reply['status'] == "OK") {
            if (api_reply['content']['isEnabled']) {
                document.getElementById("result-error").style.display = "none";
            }
            else {
                document.getElementById("result-error").innerHTML = '<div class="alert alert-warning" role="alert"><strong>Notice:</strong>ShortURL has been generated, however pending admin review.</div>';
                document.getElementById("result-error").style.display = "";
            }
            let link = location.href + api_reply['content']['surl'];
            document.getElementById("success-alert").innerHTML = "Short URL generated.";
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
    return false;
}


function generate_shorturl_mini() {
    var longURL = document.getElementById("file_url").value;
    var http = new XMLHttpRequest();
    http.open("POST", "/api/shorten", true);
    http.setRequestHeader("Content-type", "application/json");
    var params = { "url": longURL, "surl": "" };
    http.send(JSON.stringify(params));
    http.onload = function () {
        var api_reply = JSON.parse(http.responseText);
        if (api_reply['status'] == "OK" || api_reply['status'] == "FAIL") {
            let link = location.href.replace("fileHome", "") + api_reply['content']['surl'];
            document.getElementById("file_url").value = link;
            document.getElementById("success-alert").innerHTML = "File URL shortened.";
        }
    }
    return false;
}



function upload_file() {
    document.getElementById("file_expiry").value = document.getElementById("expiry_sel").value;

    let form = document.querySelector("form");
    let data = new FormData(form);

    let percent = document.getElementById("upload_btn");
    let http = new XMLHttpRequest();
    http.open("POST", "/file/upload");

    http.upload.addEventListener("progress", ({ loaded, total }) => {
        let fileLoaded = Math.floor((loaded / total) * 100);
        let fileTotal = Math.floor(total / 1000);
        let fileSize;
        (fileTotal < 1024) ? fileSize = fileTotal + " KB" : fileSize = (loaded / (1024 * 1024)).toFixed(2) + " MB";
        percent.innerHTML = fileLoaded + "%";
        if (loaded == total) {
            percent.innerHTML = "Processing...";
        }
    });

    http.send(data);
    http.onload = function () {
        if (http.responseText.length > 0) {
            percent.innerHTML = "Upload";
            var api_reply = JSON.parse(http.responseText);
            console.log(api_reply);
            if (api_reply['status'] == "OK") {
                document.getElementById("result-error").style.display = "none";
                let link = location.href.replace("/fileHome", api_reply['content']['url']);
                document.getElementById("success-alert").innerHTML = api_reply['content']['message'];
                document.getElementById("file_url").value = link
                document.getElementById("file_delete_link").value = location.href.replace("/fileHome", api_reply['content']['deleteUrl']);
                document.getElementById("qr-code").src = "/qr/" + btoa(link);
                document.getElementById("result-box").style.display = "";

                document.getElementById('slider').classList.remove('closed');
            }
            else if (api_reply['status'] == "FAIL") {
                document.getElementById("result-error").innerHTML = '<div class="alert alert-danger" role="alert"><strong>ERROR:</strong> ' + api_reply['error'] + '</div>';
                document.getElementById("result-error").style.display = "";
                document.getElementById("result-box").style.display = "none";

                document.getElementById('slider').classList.remove('closed');
            }
            else {
                alert("Server returned error.");
            }
        }
        else {
            alert("Failed contacting the server.");
        }
    }
    return false;
}

function copy_link(el) {
    var txt = "Copied to clipboard."
    var copyText = el.value;
    if (copyText != txt) {
        copyTextToClipboard(copyText);
        el.value = txt;
        setTimeout(function () { el.value = copyText; }, 2000);
    }
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



function reloadImage() {
    var rand = Math.floor((Math.random() * 1000) + 100);
    document.getElementById("user_code_img").src = "/showImage?v=" + rand;
    return false;
}