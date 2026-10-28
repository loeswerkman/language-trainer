// Serve the web app on 8081 and forward API calls to the Ktor server on 8080.
// historyApiFallback serves index.html for page URLs such as /russian/training, so refreshing works.
config.devServer = Object.assign({}, config.devServer, {
    port: 8081,
    historyApiFallback: true,
    proxy: [
        {
            context: ["/api"],
            target: "http://localhost:8080",
        },
    ],
});
