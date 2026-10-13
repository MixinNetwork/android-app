# Perps candle timeframe

The perps market detail chart defaults to 15 minutes when no saved timeframe preference exists or when the saved index is outside the available timeframe range. Every valid saved index remains selected unchanged.

Verification checklist:

- Missing preference selects 15m.
- Every valid saved index is preserved.
- Negative and upper out-of-range indices select 15m.
