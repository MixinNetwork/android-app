(function() {
    var config = {
        ethereum: {
            chainId: %1$s,
            rpcUrl: '%2$s',
            address: '%3$s',
        },
        isDebug: true
    };
    mixinwallet.ethereum = new mixinwallet.Provider(config);
    mixinwallet.postMessage = (json) => {
        window._mw_.postMessage(JSON.stringify(json));
    }
    window.ethereum = mixinwallet.ethereum;

    const mixinLogoDataUrl = 'data:image/svg+xml;base64,PHN2ZyBmaWxsPSJub25lIiBoZWlnaHQ9IjUxMiIgdmlld0JveD0iMCAwIDUxMiA1MTIiIHdpZHRoPSI1MTIiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiPjxsaW5lYXJHcmFkaWVudCBpZD0iYSIgZ3JhZGllbnRVbml0cz0idXNlclNwYWNlT25Vc2UiIHgxPSIxMy41IiB4Mj0iNTEyIiB5MT0iNTEyIiB5Mj0iLS4wMDAwMTYiPjxzdG9wIG9mZnNldD0iMCIgc3RvcC1jb2xvcj0iIzJhNWJmNiIvPjxzdG9wIG9mZnNldD0iMSIgc3RvcC1jb2xvcj0iIzUyOWZmOSIvPjwvbGluZWFyR3JhZGllbnQ+PHBhdGggZD0ibTAgMGg1MTJ2NTEyaC01MTJ6IiBmaWxsPSJ1cmwoI2EpIi8+PHBhdGggY2xpcC1ydWxlPSJldmVub2RkIiBkPSJtMzgxLjc0OSAxNTdjMi4wMDkgMCAzLjY0NCAxLjU5OCAzLjcwNSAzLjU5MmwuMDAyLjExNXYxOTEuNTg1YzAgLjU1LS4xMjMgMS4wOTQtLjM1OSAxLjU5MS0uODYxIDEuODEzLTMuMDAzIDIuNjA1LTQuODI5IDEuODA3bC0uMTEtLjA1LTQ3LjUyNC0yMi41ODRjLTIuMjE0LTEuMDUzLTMuNjQyLTMuMjYxLTMuNzAxLTUuNzAzbC0uMDAyLS4xNTd2LTE0NC42NThjMC0yLjU4OSAxLjUzOS00LjkyNSAzLjkwNS01Ljk1MWwuMTQtLjA1OSA0Ny4zNzgtMTkuMjU1Yy40NDMtLjE4LjkxNy0uMjczIDEuMzk1LS4yNzN6bS0yNTIuMDQyIDBjLjQ3OCAwIC45NTIuMDkzIDEuMzk1LjI3M2w0Ny4zNzggMTkuMjU1YzIuNDQ1Ljk5NCA0LjA0NCAzLjM3IDQuMDQ0IDYuMDF2MTQ0LjY1OGMwIDIuNTA0LTEuNDQxIDQuNzg1LTMuNzAyIDUuODZsLTQ3LjUyNSAyMi41ODRjLTEuODQ5Ljg3OS00LjA2LjA5Mi00LjkzOC0xLjc1Ny0uMjM2LS40OTctLjM1OS0xLjA0MS0uMzU5LTEuNTkxdi0xOTEuNTg1YzAtMi4wNDcgMS42NTktMy43MDcgMy43MDctMy43MDd6bTEzMC4xOTEgNDEuODQ3IDQzLjgyMSAyNS4zMDRjMi41ODEgMS40OSA0LjE3IDQuMjQ0IDQuMTcgNy4yMjR2NTAuNjA3YzAgMi45OC0xLjU4OSA1LjczNC00LjE3IDcuMjI0bC00My44MjEgMjUuMzA0Yy0yLjU4MSAxLjQ4OS01Ljc2IDEuNDg5LTguMzQgMGwtNDMuODIyLTI1LjMwNGMtMi41OC0xLjQ5LTQuMTctNC4yNDQtNC4xNy03LjIyNHYtNTAuNjA3YzAtMi45OCAxLjU5LTUuNzM0IDQuMTctNy4yMjRsNDMuODIyLTI1LjMwNGMyLjU4LTEuNDkgNS43NTktMS40OSA4LjM0IDB6IiBmaWxsPSIjZmZmIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiLz48L3N2Zz4=';
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