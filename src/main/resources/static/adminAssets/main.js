
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

function admin_get_file_logs(downloadKey) {
    var divBox = document.getElementById('logsTable');

    var http = new XMLHttpRequest();
    http.open("GET", "/admin/fileLogs/" + downloadKey, true);
    http.send();
    http.onload = function () {
        divBox.innerHTML = http.responseText;
    }
    divBox.classList.remove('closed');
    return false;
}

function admin_get_history() {
    var divBox = document.getElementById('historyTable');

    var parts = location.href.split("/");
    var tableType = "urlsTable";
    if(parts[parts.length -1] == "file")
        tableType = "filesTable";

    var http = new XMLHttpRequest();
    http.open("GET", "/admin/" + tableType, true);
    http.send();
    http.onload = function () {
        divBox.innerHTML = http.responseText;
    }
    divBox.classList.remove('closed');
    return false;
}

function admin_delete(deleteUrl) {
    var http = new XMLHttpRequest();
    http.open("GET", "/api" + deleteUrl, true);
    http.send();
    http.onload = function () {
        var api_reply = JSON.parse(http.responseText);
        if (api_reply['status'] == "OK")
            admin_get_history();
        else
            alert("Failed to delete.");
    }
}