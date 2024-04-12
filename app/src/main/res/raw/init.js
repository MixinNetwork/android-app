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
        window._mw_.postMessage(JSON.stringify(json));
    }
    window.ethereum = mixinwallet.ethereum;


    const mixinLogoDataUrl = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNTEyIiBoZWlnaHQ9IjUxMiIgdmlld0JveD0iMCAwIDUxMiA1MTIiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSI1MTIiIGhlaWdodD0iNTEyIiByeD0iMjU2IiBmaWxsPSJ1cmwoI3BhaW50MF9saW5lYXJfMjc2NV8yKSIvPgo8cGF0aCBmaWxsLXJ1bGU9ImV2ZW5vZGQiIGNsaXAtcnVsZT0iZXZlbm9kZCIgZD0iTTM4MS43NDkgMTU3QzM4My43NTggMTU3IDM4NS4zOTMgMTU4LjU5OCAzODUuNDU0IDE2MC41OTJMMzg1LjQ1NiAxNjAuNzA3VjM1Mi4yOTJDMzg1LjQ1NiAzNTIuODQyIDM4NS4zMzMgMzUzLjM4NiAzODUuMDk3IDM1My44ODNDMzg0LjIzNiAzNTUuNjk2IDM4Mi4wOTQgMzU2LjQ4OCAzODAuMjY4IDM1NS42OUwzODAuMTU4IDM1NS42NEwzMzIuNjM0IDMzMy4wNTZDMzMwLjQyIDMzMi4wMDMgMzI4Ljk5MiAzMjkuNzk1IDMyOC45MzMgMzI3LjM1M0wzMjguOTMxIDMyNy4xOTZWMTgyLjUzOEMzMjguOTMxIDE3OS45NDkgMzMwLjQ3IDE3Ny42MTMgMzMyLjgzNiAxNzYuNTg3TDMzMi45NzYgMTc2LjUyOEwzODAuMzU0IDE1Ny4yNzNDMzgwLjc5NyAxNTcuMDkzIDM4MS4yNzEgMTU3IDM4MS43NDkgMTU3Wk0xMjkuNzA3IDE1N0MxMzAuMTg1IDE1NyAxMzAuNjU5IDE1Ny4wOTMgMTMxLjEwMiAxNTcuMjczTDE3OC40OCAxNzYuNTI4QzE4MC45MjUgMTc3LjUyMiAxODIuNTI0IDE3OS44OTggMTgyLjUyNCAxODIuNTM4VjMyNy4xOTZDMTgyLjUyNCAzMjkuNyAxODEuMDgzIDMzMS45ODEgMTc4LjgyMiAzMzMuMDU2TDEzMS4yOTcgMzU1LjY0QzEyOS40NDggMzU2LjUxOSAxMjcuMjM3IDM1NS43MzIgMTI2LjM1OSAzNTMuODgzQzEyNi4xMjMgMzUzLjM4NiAxMjYgMzUyLjg0MiAxMjYgMzUyLjI5MlYxNjAuNzA3QzEyNiAxNTguNjYgMTI3LjY1OSAxNTcgMTI5LjcwNyAxNTdaTTI1OS44OTggMTk4Ljg0N0wzMDMuNzE5IDIyNC4xNTFDMzA2LjMgMjI1LjY0MSAzMDcuODg5IDIyOC4zOTUgMzA3Ljg4OSAyMzEuMzc1VjI4MS45ODJDMzA3Ljg4OSAyODQuOTYyIDMwNi4zIDI4Ny43MTYgMzAzLjcxOSAyODkuMjA2TDI1OS44OTggMzE0LjUxQzI1Ny4zMTcgMzE1Ljk5OSAyNTQuMTM4IDMxNS45OTkgMjUxLjU1OCAzMTQuNTFMMjA3LjczNiAyODkuMjA2QzIwNS4xNTYgMjg3LjcxNiAyMDMuNTY2IDI4NC45NjIgMjAzLjU2NiAyODEuOTgyVjIzMS4zNzVDMjAzLjU2NiAyMjguMzk1IDIwNS4xNTYgMjI1LjY0MSAyMDcuNzM2IDIyNC4xNTFMMjUxLjU1OCAxOTguODQ3QzI1NC4xMzggMTk3LjM1NyAyNTcuMzE3IDE5Ny4zNTcgMjU5Ljg5OCAxOTguODQ3WiIgZmlsbD0id2hpdGUiLz4KPGRlZnM+CjxsaW5lYXJHcmFkaWVudCBpZD0icGFpbnQwX2xpbmVhcl8yNzY1XzIiIHgxPSIxMy41IiB5MT0iNTEyIiB4Mj0iNTEyIiB5Mj0iLTEuNjE4NzNlLTA1IiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+CjxzdG9wIHN0b3AtY29sb3I9IiMyQTVCRjYiLz4KPHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjNTI5RkY5Ii8+CjwvbGluZWFyR3JhZGllbnQ+CjwvZGVmcz4KPC9zdmc+Cg==';
    const info = {
        uuid: crypto.randomUUID(),
        name: 'Mixin Messenger',
        icon: mixinLogoDataUrl,
        rdns: 'one.mixin.messenger',
    };

    function initializeEIP6963(provider, options = {}) {
        const providerDetail = { info, provider };
        Object.defineProperty(providerDetail, 'provider', {
            get() {
                options.onAccessProvider?.();
                return provider;
            },
        });
        const announceEvent = new CustomEvent('eip6963:announceProvider', {
        detail: Object.freeze(providerDetail),
    });
    window.dispatchEvent(announceEvent);
    window.addEventListener('eip6963:requestProvider', () => {
        window.dispatchEvent(announceEvent);
        options.onRequestProvider?.();
    });
    }
    initializeEIP6963(window.ethereum);
})();