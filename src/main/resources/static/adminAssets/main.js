
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

