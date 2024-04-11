 (function() {
            var config = {
                ethereum: {
                    chainId: %1$s,
                    rpcUrl: '%2$s'
                },
                solana: {
                    cluster: "mainnet-beta",
                },
                isDebug: true
            };
            mixinwallet.ethereum = new mixinwallet.Provider(config);
            mixinwallet.solana = new mixinwallet.SolanaProvider(config);
            mixinwallet.postMessage = (json) => {
                window._tw_.postMessage(JSON.stringify(json));
            }
            window.ethereum = mixinwallet.ethereum;
        })();