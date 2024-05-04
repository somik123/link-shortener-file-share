[![Docker Image Build](https://github.com/somik123/link-shortener-file-share/actions/workflows/main.yaml/badge.svg)](https://github.com/somik123/link-shortener-file-share/actions/workflows/main.yaml)
# link-shortener-file-share
A simple link shortener & time based file share service written in Java Spring Boot

Set the following environment variables if not using official docker image:
```
SHORTURL_DB_HOST - MySQL database host, usually localhost
SHORTURL_DB_NAME - MySQL database name. Note that the database must exist. Tables will be autocreated on boot (if not exist)
SHORTURL_DB_USER - MySQL database usernme
SHORTURL_DB_PASS - MySQL database password

SHORTURL_USER - Admin username to manage all shorturls and uploaded files
SHORTURL_PASS - Admin password to manage all shorturls and uploaded files
SHORTURL_PASS_HIDE - Set this to "yes" to disable printing the password to logs when starting up the app

SHORTURL_LENGTH - Length of generated shorturls (if auto-generted)
```

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
- [ ] Test for vulnerabilities.

