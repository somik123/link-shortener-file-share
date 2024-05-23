[![Docker Image Build](https://github.com/somik123/link-shortener-file-share/actions/workflows/main.yaml/badge.svg)](https://github.com/somik123/link-shortener-file-share/actions/workflows/main.yaml)

# link-shortener-file-share
A simple link shortener & time based file share service written in Java Spring Boot

### Installation
Copy/download the `docker-compose.yml` file and run it. You do not require any of the other source files unless you want to build the image yourself.
```
curl -o docker-compose.yml https://raw.githubusercontent.com/somik123/link-shortener-file-share/main/docker-compose.yml
```

Edit the file with your favorit editor to set all the environment variables in the docker-compose file, including the admin username/password:

`SHORTURL_DB_HOST` - MySQL database host, usually localhost
`SHORTURL_DB_NAME` - MySQL database name. Note that the database must exist. Tables will be autocreated on boot (if not exist)
`SHORTURL_DB_USER` - MySQL database usernme
`SHORTURL_DB_PASS` - MySQL database password

`SHORTURL_LENGTH` - Length of generated shorturls (if auto-generted)
`FILEURL_LENGTH`  - Length of generated file urls (if auto-generted)

`TELEGRAM_APIKEY`  - Telegram API key that will be used to send notifications to admin
`TELEGRAM_ADMINID`  - Admin's chat id. This can be a group id as well. See: https://docs.tracardi.com/qa/how_can_i_get_telegram_bot/
`TELEGRAM_AUTHKEY` - (Optional) Authorization key to be used by telegram and this app to validate requests are coming from telegram.
`SITE_FULL_URL`    - Full url to the home page of the site with trailing slash, like: https://www.example.com/


`SHORTURL_USER`      - Admin username to manage all shorturls and uploaded files
`SHORTURL_PASS`      - Admin password to manage all shorturls and uploaded files
`SHORTURL_PASS_HIDE` - Set this to "yes" to disable printing the password to logs when starting up the app

`UPLOADFILE_MAX_SIZE` - Max allowed size for uploaded file (in MB)



Once done, save the file and run the following command from the same folder as your `docker-compose.yml` file.
```
docker compose up -d
```

The website will be on port `6080` on the server you run it on. Use nginx proxy to secure it with https.

Default user details (if not set from ENV variables):
- Username: `user`
- Password: `password`

Set the environmental variable `BUCKET_PASS_HIDE` to `no` to display the admin username and password in the log file during boot.

<br>

### DB Manager
PhpMyAdmin database manager is disabled by default in the `docker-compose.yml` file. It provides a simple way to troubleshoot or edit your bucket database but use it at your own risk.

To use it, copy paste the following code block at the bottom of your `docker-compose.yml` file and run the `docker compose up -d` command.

```
  db_manager:
    image: phpmyadmin
    container_name: phpmyadmin
    restart: unless-stopped
    environment:
      TZ: Asia/Singapore
      PMA_HOST: db
    ports:
      - 6088:80
    depends_on:
      - db
    links:
      - db
```
It is available on port `6088` once it is up. 

<br>

### Screenshots

<img src="https://raw.githubusercontent.com/somik123/link-shortener-file-share/main/screenshots/1.png">

<img src="https://raw.githubusercontent.com/somik123/link-shortener-file-share/main/screenshots/2.png">

<img src="https://raw.githubusercontent.com/somik123/link-shortener-file-share/main/screenshots/3.png">

<img src="https://raw.githubusercontent.com/somik123/link-shortener-file-share/main/screenshots/4.png">

<img src="https://raw.githubusercontent.com/somik123/link-shortener-file-share/main/screenshots/5.png">

<img src="https://raw.githubusercontent.com/somik123/link-shortener-file-share/main/screenshots/6.png">

<img src="https://raw.githubusercontent.com/somik123/link-shortener-file-share/main/screenshots/7.png">

<br>

### To-do list
- [x] Add env variables for db settings.
- [x] Ability to generate shorturls & visit them.
- [x] Able to delete shorturls.
- [x] Add admin section to manage shorturls.
- [x] Add API calls to generate shorturls.
- [x] Add file upload & share feature.
- [x] Shorten file upload links using url shortener.
- [x] Expire/delete the files after predefined time.
- [x] Write a dockerfile to build the app for deployment.
- [x] Write a docker compose script to build and run the stack.
- [x] Add telegram bot for notifications of new events.
- [x] Add link/file validations through telegram bot.
- [x] Add uploaded file max size limits.
- [x] Add uploaded file url size customization.
- [x] Add shorturl url size customization.
- [x] Added contact form for reports (report sent via telegram to admin).
- [x] Send message via telegram asynchronously to save time.
- [x] Add custom captcha code to the contact form.
- [x] Allow urls to be shortened via telegram messages
- [x] Allow files to be uploaded via telegram messages
- [x] Allow file url shortening via telegram messages after upload
- [x] Allow files/shorturls deletion via telegram commands
- [ ] Test for vulnerabilities.

