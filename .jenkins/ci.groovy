@Library('add-ons-shared-libs@develop') _

node {
    continuousIntegrationPipeline(
        sonar: [
            enable: true,
            projectKey: "eclipse-kura_kura-networking",
            tokenId: "sonarcloud-token-kura-networking",
            exclusions: "tests/**/*.java,org.eclipse.kura.nm/src/main/java/org/freedesktop/**/*,org.eclipse.kura.nm/src/main/java/fi/w1/**/*"
        ],
    )
}
