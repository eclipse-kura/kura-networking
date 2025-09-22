@Library('add-ons-shared-libs@develop') _

node {
    continuousIntegrationPipeline(
        buildType: "deploy",
        sonar: [
            enable: true,
            projectKey: "eclipse-kura_kura-networking",
            tokenId: "sonarcloud-token-kura-networking",
            exclusions: "tests/**/*.java,bundles/org.eclipse.kura.nm/src/main/java/org/freedesktop/**/*,bundles/org.eclipse.kura.nm/src/main/java/fi/w1/**/*"
        ],
    )
}
