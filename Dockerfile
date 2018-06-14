FROM clojure:alpine
RUN mkdir -p /opt/bot-giphy
RUN mkdir -p /etc/opt/bot-giphy
WORKDIR /opt/bot-giphy
COPY project.clj /opt/bot-giphy/
RUN lein deps
COPY . /opt/bot-giphy/
RUN mv "$(lein do git-info-edn, uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" bot-giphy-standalone.jar
CMD ["java", "-jar", "/opt/bot-giphy/bot-giphy-standalone.jar", "-c", "/etc/opt/bot-giphy/config.edn"]
