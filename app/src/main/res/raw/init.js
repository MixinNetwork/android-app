(function() {
    var config = {
        ethereum: {
            chainId: %1$s,
            rpcUrl: '%2$s',
            address: '%3$s',
        },
        solana: {
            cluster: "mainnet-beta",
        },
        isDebug: false
    };
    mixinwallet.ethereum = new mixinwallet.Provider(config);
    mixinwallet.solana = new mixinwallet.SolanaProvider(config);
    mixinwallet.postMessage = (json) => {
        window._mw_.postMessage(JSON.stringify(json));
    }
    window.ethereum = mixinwallet.ethereum;

    const mixinLogoDataUrl = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNTEyIiBoZWlnaHQ9IjUxMiIgdmlld0JveD0iMCAwIDUxMiA1MTIiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxwYXRoIGQ9Ik0wIDExMkMwIDUwLjE0NDEgNTAuMTQ0MSAwIDExMiAwSDQwMEM0NjEuODU2IDAgNTEyIDUwLjE0NDEgNTEyIDExMlY0MDBDNTEyIDQ2MS44NTYgNDYxLjg1NiA1MTIgNDAwIDUxMkgxMTJDNTAuMTQ0MSA1MTIgMCA0NjEuODU2IDAgNDAwVjExMloiIGZpbGw9InVybCgjcGFpbnQwX2xpbmVhcl8yNzk1XzE3KSIvPgo8cGF0aCBmaWxsLXJ1bGU9ImV2ZW5vZGQiIGNsaXAtcnVsZT0iZXZlbm9kZCIgZD0iTTM4MS43NDkgMTU2QzM4My43NTggMTU2IDM4NS4zOTMgMTU3LjU5OCAzODUuNDU0IDE1OS41OTJMMzg1LjQ1NiAxNTkuNzA3VjM1MS4yOTJDMzg1LjQ1NiAzNTEuODQyIDM4NS4zMzMgMzUyLjM4NiAzODUuMDk3IDM1Mi44ODNDMzg0LjIzNiAzNTQuNjk2IDM4Mi4wOTQgMzU1LjQ4OCAzODAuMjY4IDM1NC42OUwzODAuMTU4IDM1NC42NEwzMzIuNjM0IDMzMi4wNTZDMzMwLjQyIDMzMS4wMDMgMzI4Ljk5MiAzMjguNzk1IDMyOC45MzMgMzI2LjM1M0wzMjguOTMxIDMyNi4xOTZWMTgxLjUzOEMzMjguOTMxIDE3OC45NDkgMzMwLjQ3IDE3Ni42MTMgMzMyLjgzNiAxNzUuNTg3TDMzMi45NzYgMTc1LjUyOEwzODAuMzU0IDE1Ni4yNzNDMzgwLjc5NyAxNTYuMDkzIDM4MS4yNzEgMTU2IDM4MS43NDkgMTU2Wk0xMjkuNzA3IDE1NkMxMzAuMTg1IDE1NiAxMzAuNjU5IDE1Ni4wOTMgMTMxLjEwMiAxNTYuMjczTDE3OC40OCAxNzUuNTI4QzE4MC45MjUgMTc2LjUyMiAxODIuNTI0IDE3OC44OTggMTgyLjUyNCAxODEuNTM4VjMyNi4xOTZDMTgyLjUyNCAzMjguNyAxODEuMDgzIDMzMC45ODEgMTc4LjgyMiAzMzIuMDU2TDEzMS4yOTcgMzU0LjY0QzEyOS40NDggMzU1LjUxOSAxMjcuMjM3IDM1NC43MzIgMTI2LjM1OSAzNTIuODgzQzEyNi4xMjMgMzUyLjM4NiAxMjYgMzUxLjg0MiAxMjYgMzUxLjI5MlYxNTkuNzA3QzEyNiAxNTcuNjYgMTI3LjY1OSAxNTYgMTI5LjcwNyAxNTZaTTI1OS44OTggMTk3Ljg0N0wzMDMuNzE5IDIyMy4xNTFDMzA2LjMgMjI0LjY0MSAzMDcuODg5IDIyNy4zOTUgMzA3Ljg4OSAyMzAuMzc1VjI4MC45ODJDMzA3Ljg4OSAyODMuOTYyIDMwNi4zIDI4Ni43MTYgMzAzLjcxOSAyODguMjA2TDI1OS44OTggMzEzLjUxQzI1Ny4zMTcgMzE0Ljk5OSAyNTQuMTM4IDMxNC45OTkgMjUxLjU1OCAzMTMuNTFMMjA3LjczNiAyODguMjA2QzIwNS4xNTYgMjg2LjcxNiAyMDMuNTY2IDI4My45NjIgMjAzLjU2NiAyODAuOTgyVjIzMC4zNzVDMjAzLjU2NiAyMjcuMzk1IDIwNS4xNTYgMjI0LjY0MSAyMDcuNzM2IDIyMy4xNTFMMjUxLjU1OCAxOTcuODQ3QzI1NC4xMzggMTk2LjM1NyAyNTcuMzE3IDE5Ni4zNTcgMjU5Ljg5OCAxOTcuODQ3WiIgZmlsbD0id2hpdGUiLz4KPGRlZnM+CjxsaW5lYXJHcmFkaWVudCBpZD0icGFpbnQwX2xpbmVhcl8yNzk1XzE3IiB4MT0iMTMuNSIgeTE9IjUxMiIgeDI9IjUxMiIgeTI9Ii0xLjYxODczZS0wNSIgZ3JhZGllbnRVbml0cz0idXNlclNwYWNlT25Vc2UiPgo8c3RvcCBzdG9wLWNvbG9yPSIjMkE1QkY2Ii8+CjxzdG9wIG9mZnNldD0iMSIgc3RvcC1jb2xvcj0iIzUyOUZGOSIvPgo8L2xpbmVhckdyYWRpZW50Pgo8L2RlZnM+Cjwvc3ZnPgo=';
    const info = {
        uuid: crypto.randomUUID(),
        name: 'Mixin Messenger',
        icon: mixinLogoDataUrl,
        rdns: 'one.mixin.messenger',
    };

    window.ethereum.setAddress(config.ethereum.address);

    function initializeEIP6963(provider, options = {}) {
        const providerDetail = {
            info,
            provider
        };
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