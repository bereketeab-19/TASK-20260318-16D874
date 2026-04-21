FROM alpine:3.21

RUN apk add --no-cache mysql-client tzdata ca-certificates && update-ca-certificates

COPY backup.sh /usr/local/bin/backup.sh
COPY retention.sh /usr/local/bin/retention.sh
COPY crontab /etc/crontabs/root

RUN chmod +x /usr/local/bin/backup.sh /usr/local/bin/retention.sh

CMD ["crond","-f","-l","2"]

