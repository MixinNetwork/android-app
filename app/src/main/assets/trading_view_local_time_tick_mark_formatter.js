window.localTimeTickMarkFormatter = () => {
    return (time, tickMarkType, locale) => {
        if (typeof time !== "number") {
            return null
        }

        const formatOptions = {}
        switch (tickMarkType) {
            case 0:
                formatOptions.year = "numeric"
                break
            case 1:
                formatOptions.month = "short"
                break
            case 2:
                formatOptions.day = "numeric"
                break
            case 3:
                formatOptions.hour12 = false
                formatOptions.hour = "2-digit"
                formatOptions.minute = "2-digit"
                break
            case 4:
                formatOptions.hour12 = false
                formatOptions.hour = "2-digit"
                formatOptions.minute = "2-digit"
                formatOptions.second = "2-digit"
                break
        }
        return new Date(time * 1000).toLocaleString(locale, formatOptions)
    }
}
