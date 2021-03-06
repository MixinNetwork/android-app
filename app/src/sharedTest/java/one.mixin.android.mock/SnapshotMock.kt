package one.mixin.android.mock

const val MOCK_SNAPSHOTS = """
  [
    {
      "type": "transfer",
      "snapshot_id": "6c076917-3e8c-4ea0-853b-d460d9aa77a5",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "dd4f9ab4-f214-4fc1-9fe3-225bafb8cf22",
      "memo": "memo for test",
      "created_at": "2020-09-24T00:07:25.837303Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "827e407b-9473-4cc8-834b-3770328a0c8d",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "052eebf6-9a25-42cb-b434-20d29d269928",
      "memo": "memo for test",
      "created_at": "2020-09-19T00:17:02.037499Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "deposit",
      "snapshot_id": "48e4613a-a7a2-42e2-a3f1-90a184c15e31",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "31e72975-5eaf-44d3-abb3-276c8b14b282",
      "memo": "memo for test",
      "created_at": "2020-09-14T00:42:27.4308Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "deposit",
      "snapshot_id": "4f0050cc-1f0b-4a41-9518-269da596c605",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "51bea707-2ae3-42ee-b92b-28ced95ad327",
      "memo": "memo for test",
      "created_at": "2020-09-14T00:42:25.961549Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "withdrawal",
      "snapshot_id": "02cebeb8-7db4-4c1e-b04a-4a6ed22a50a9",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "66213d71-5d0a-4e9f-be67-ded0c298afde",
      "memo": "memo for test",
      "created_at": "2020-09-10T00:15:14.660925Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "withdrawal",
      "snapshot_id": "fc6f72ad-9132-47d3-a1b0-61880efa102a",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "6b0b3ef0-524a-4133-b2a2-1c68081d4532",
      "memo": "memo for test",
      "created_at": "2020-09-09T00:31:13.434455Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "fee",
      "snapshot_id": "4aca631b-b5be-46b4-bc98-fa9caeacb324",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "0186a2ba-378f-40d9-b7df-14ffcb122770",
      "memo": "memo for test",
      "created_at": "2020-09-09T00:08:13.694004Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "fee",
      "snapshot_id": "4e8f63c6-072a-4c6b-a32d-d266c77cee3a",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "60e67c5a-6ac9-4350-acec-08a284a17436",
      "memo": "memo for test",
      "created_at": "2020-09-05T00:01:50.371692Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "rebate",
      "snapshot_id": "1ec1b0a3-ecf0-4624-b96a-78ae51ccacc7",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "cd533021-0492-4d5e-a0f6-5e524319c71d",
      "memo": "memo for test",
      "created_at": "2020-09-02T00:15:01.748808Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "rebate",
      "snapshot_id": "415ec587-bb97-44ca-a21a-f5df381eb4fe",
      "opponent_id": "2fa3f350-d32a-4289-b92d-39c7c5844150",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "b29971ff-7aa2-3fed-88a6-032468a60d06",
      "memo": "memo for test",
      "created_at": "2020-08-31T01:34:39.150364Z",
      "counter_user_id": "2fa3f350-d32a-4289-b92d-39c7c5844150"
    },
    {
      "type": "raw",
      "snapshot_id": "0580a333-6343-4579-b0b7-713ca99ee13b",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "e2d7f7af-4406-4f1c-810f-f1d7bc8d9244",
      "memo": "memo for test",
      "created_at": "2020-08-29T00:13:27.450652Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "raw",
      "snapshot_id": "98ad5205-96a9-44db-9bdc-f8d420e746a4",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "1e285516-4b33-4afa-81d9-b0eeb4c0d0b0",
      "memo": "memo for test",
      "created_at": "2020-08-29T00:13:25.545372Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "pending",
      "snapshot_id": "f9ca2924-8fd8-4f5e-bac1-fdf1316625d3",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "confirmations" : "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "0a3246a3-8970-41a7-b55c-a5dd75e5ee10",
      "memo": "memo for test",
      "created_at": "2020-08-26T00:27:06.29884Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "pending",
      "snapshot_id": "339f0a1d-e8b2-4e9e-9d32-1cff674b767d",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "confirmations" : "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "8e6a0143-3e0b-45e1-af97-68ab4fff7a42",
      "memo": "memo for test",
      "created_at": "2020-08-23T00:17:03.394113Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "e0f24f5e-9251-42a8-9ffc-c317b8ec3e4b",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "e4227e24-a218-46af-87fe-0448403ae68e",
      "memo": "memo for test",
      "created_at": "2020-08-21T01:53:36.424354Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "6894e0a1-14d6-40b9-a967-15387f43bcdb",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "f402db6d-852b-4273-8b69-cd56f7062c8b",
      "memo": "memo for test",
      "created_at": "2020-08-18T00:14:04.508108Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "fd67e415-5330-413f-a1a9-4e60fb129f48",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "795c497f-b8be-3aad-9611-4ca6074f7c9b",
      "memo": "memo for test",
      "created_at": "2020-08-17T00:10:43.847836Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "0189750a-47a1-4c5b-b50c-89bd9276980f",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "5c9d2a7a-1cd4-4c27-b9f8-cd252f6fe09f",
      "memo": "memo for test",
      "created_at": "2020-08-17T00:10:43.557995Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "2df0b3b2-c4da-417a-ac90-54ac9ae61bd1",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "67cab42a-aa39-4697-bbaa-2fdadd0a8354",
      "memo": "memo for test",
      "created_at": "2020-08-13T00:07:03.908598Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "4c7ef37e-1c31-47fa-b40b-8ada82642fb1",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "8d5407cd-df05-4c23-bafa-b73db15a51ae",
      "memo": "memo for test",
      "created_at": "2020-08-13T00:07:01.057426Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "ee63c527-7cbe-4450-a63e-85735f790573",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "6336df54-062c-4bf1-ad38-58a40e1fe931",
      "memo": "memo for test",
      "created_at": "2020-08-09T00:10:14.586732Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "fe5d388a-ea1c-4948-8a26-41aad659fad0",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "409e2516-e9d1-44b4-aa68-e7ded254738d",
      "memo": "memo for test",
      "created_at": "2020-08-06T00:35:16.307652Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "012fc405-24ed-4a7e-a284-1002f2217ea0",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "e0ca9444-de1e-4e78-a86f-ad7cc389b171",
      "memo": "memo for test",
      "created_at": "2020-08-03T00:10:26.606332Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "56dfbe70-6728-458d-b05a-5d17ecad9780",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "7a0cfa0f-91d2-1df1-a8f0-4532dfbd6cd7",
      "memo": "memo for test",
      "created_at": "2020-07-31T00:32:47.292045Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "7f1d0bc3-6b37-4189-bbaf-fe20556939c7",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "5fa0840c-af52-4534-85d7-395e6a2e5196",
      "memo": "memo for test",
      "created_at": "2020-07-30T00:12:06.94935Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "573c1ee2-7533-419f-be72-b53d9f8f1a24",
      "opponent_id": "67a87828-18f5-46a1-b6cc-c72a97a77c43",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "8896b886-dfdb-22f3-aa95-6d11b0c8a0e7",
      "memo": "memo for test",
      "created_at": "2020-07-27T17:44:59.010493Z",
      "counter_user_id": "67a87828-18f5-46a1-b6cc-c72a97a77c43"
    },
    {
      "type": "transfer",
      "snapshot_id": "66ded2e9-0116-4b73-b81d-af352498d5d6",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "da51bd05-5f99-481b-8e9f-04c13579d1f8",
      "memo": "memo for test",
      "created_at": "2020-07-26T00:04:54.420069Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "7f518ab3-fe2a-4e0e-ae1e-875d83809dac",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "a4e5c099-d436-483a-810f-198760344867",
      "memo": "memo for test",
      "created_at": "2020-07-25T00:10:14.624Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "2a40496b-a006-4d79-9114-3bf568b8d2e8",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "76e99611-721c-4603-b4e8-d3ab5234036e",
      "memo": "memo for test",
      "created_at": "2020-07-24T00:10:40.99348Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    },
    {
      "type": "transfer",
      "snapshot_id": "a9d0a8cf-a0c6-4479-86f3-5c56661aea68",
      "opponent_id": "$MOCK_ME_USER_ID",
      "asset_id": "c6d1c728-2624-429b-8e0d-d9d19b6592fa",
      "amount": "1",
      "opening_balance": "10",
      "closing_balance": "10",
      "trace_id": "c656c5a2-113e-47e2-930b-aa2e9496d00e",
      "memo": "memo for test",
      "created_at": "2020-07-20T00:05:00.997593Z",
      "counter_user_id": "$MOCK_ME_USER_ID"
    }
  ]
"""
