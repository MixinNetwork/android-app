package one.mixin.android.mock

const val MOCK_USERS_JSON = """
[
  {
    "type": "user",
    "user_id": $MOCK_ME_USER_ID,
    "identity_number": "12345",
    "phone": "+8613123456789",
    "full_name": "Test Me",
    "biography": "my biography for test",
    "avatar_url": "",
    "relationship": "ME",
    "mute_until": "0001-01-01T00:00:00Z",
    "created_at": "2020-11-17T15:16:13.998559422Z",
    "is_verified": false,
    "is_scam": false
  },
  {
    "type": "user",
    "user_id": "7B704160-DC2C-4B37-A3CC-3171A7086D4C",
    "identity_number": "12346",
    "phone": "+8613123456788",
    "full_name": "Test user1",
    "biography": "user1 biography for test",
    "avatar_url": "",
    "relationship": "FRIEND",
    "mute_until": "0001-01-01T00:00:00Z",
    "created_at": "2020-11-17T15:16:13.998559422Z",
    "is_verified": false,
    "is_scam": false
  },
  {
    "type": "user",
    "user_id": "88D9F657-B253-4F4D-8962-FC60E9F3B8CB",
    "identity_number": "12347",
    "phone": "+8613123456787",
    "full_name": "Test user2",
    "biography": "user2 biography for test",
    "avatar_url": "",
    "relationship": "FRIEND",
    "mute_until": "0001-01-01T00:00:00Z",
    "created_at": "2020-11-17T15:16:13.998559422Z",
    "is_verified": false,
    "is_scam": false
  },
  {
    "type": "user",
    "user_id": "0676CD74-BEF2-48AF-8D06-B29606359067",
    "identity_number": "12348",
    "phone": "+8613123456786",
    "full_name": "Test user3 STRANGER",
    "biography": "user3 biography for test",
    "avatar_url": "",
    "relationship": "STRANGER",
    "mute_until": "0001-01-01T00:00:00Z",
    "created_at": "2020-11-17T15:16:13.998559422Z",
    "is_verified": false,
    "is_scam": false
  },
  {
    "type": "user",
    "user_id": "2399CF50-3371-48E8-BE45-1FA7041B4426",
    "identity_number": "12340",
    "phone": "+8613123456785",
    "full_name": "Test user4 BLOCKING",
    "biography": "user4 biography for test",
    "avatar_url": "",
    "relationship": "BLOCKING",
    "mute_until": "0001-01-01T00:00:00Z",
    "created_at": "2020-11-17T15:16:13.998559422Z",
    "is_verified": false,
    "is_scam": false
  }
]
"""
