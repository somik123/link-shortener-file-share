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

`SHORTURL_USER` - Admin username to manage all shorturls and uploaded files
`SHORTURL_PASS` - Admin password to manage all shorturls and uploaded files
`SHORTURL_PASS_HIDE` - Set this to "yes" to disable printing the password to logs when starting up the app

`SHORTURL_LENGTH` - Length of generated shorturls (if auto-generted)

`UPLOADFILE_MAX_SIZE` - Max allowed size for uploaded file (in MB)

`TELEGRAM_APIKEY` - Telegram API key that will be used to send notifications to admin
`TELEGRAM_CHATID` - Admin's chat id. This can be a group id as well. See: https://docs.tracardi.com/qa/how_can_i_get_telegram_bot/

`SITE_FULL_URL` - Full url to the home page of the site with trailing slash, like: https://www.example.com/

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

### To-do list
- [x] Add env variables for db settings.
- [x] Ability to generate shorturls & visit them
- [x] Able to delete shorturls
- [x] Add admin section to manage shorturls
- [x] Add API calls to generate shorturls
- [x] Add file upload & share feature
- [x] Shorten file upload links using url shortener
- [x] Expire/delete the files after predefined time
- [x] Write a dockerfile to build the app for deployment.
- [x] Write a docker compose script to build and run the stack.
- [x] Add telegram bot for notifications of new events.
- [x] Add link/file validations through telegram bot.
- [x] Add uploaded file max size limits.
- [ ] Test for vulnerabilities.

