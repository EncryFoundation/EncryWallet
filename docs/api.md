__GET__ _/wallet/all_\
Returns all wallets.

_Example response:_

```json
[
    {
        "pubKey": "BF8oakaAqmf8XATDnANhDfF1F5u3hnvyZFR6VjDKYuE6",
        "address": "46mz32uZDhmuwqs5Kk3NJ91DQwEctVTnawM3S3HYB1NxaYcBAW"
    },
    {
        "pubKey": "AVFLqPfnkSz41HgEw8ZYrQkG4fs58PwF8G1tPWF3vkxt",
        "address": "41poSCtCMbNs2qvZ1dmuU92aLgCV3Y9XTZwcdGLWYt41KuR1eq"
    }
]
```

__GET__ _/wallet/all/info_\
Returns all wallets with their balance

_Example response:_

```json
[
    {
            "wallet": {
                "pubKey": "BF8oakaAqmf8XATDnANhDfF1F5u3hnvyZFR6VjDKYuE6",
                "address": "46mz32uZDhmuwqs5Kk3NJ91DQwEctVTnawM3S3HYB1NxaYcBAW"
            },
            "balance": 2
        },
        {
            "wallet": {
                "pubKey": "AVFLqPfnkSz41HgEw8ZYrQkG4fs58PwF8G1tPWF3vkxt",
                "address": "41poSCtCMbNs2qvZ1dmuU92aLgCV3Y9XTZwcdGLWYt41KuR1eq"
            },
            "balance": 2
        }
    }
]
```

__POST__ _/wallet/create_\
Creates a new wallet. Accepts optional key seed as a request body.

_Example response:_
```json
{
    "pubKey": "AVFLqPfnkSz41HgEw8ZYrQkG4fs58PwF8G1tPWF3vkxt",
    "address": "41poSCtCMbNs2qvZ1dmuU92aLgCV3Y9XTZwcdGLWYt41KuR1eq"
}
```

__POST__ _/wallet/restore/withSecret_\
Restores wallet with its secret key. The key is accepted as a `secretKey` query parameter.

_Example response:_
```json
{
    "pubKey": "AVFLqPfnkSz41HgEw8ZYrQkG4fs58PwF8G1tPWF3vkxt",
    "address": "41poSCtCMbNs2qvZ1dmuU92aLgCV3Y9XTZwcdGLWYt41KuR1eq"
}
```

__POST__ _/transactions/send/payment/:walletId_\
Send payment transaction to the node. Responses with a plan body and a status code representing whether the operation is successful

_Example request:_
```json
{
    "fee": 12,
    "amount": 234134,
    "recipient": "41poSCtCMbNs2qvZ1dmuU92aLgCV3Y9XTZwcdGLWYt41KuR1eq"
}
```

__POST__ _/transactions/send/scripted/:walletId_\
Send scripted transaction to the node. Responses with a plan body and a status code representing whether the operation is successful

_Example request:_
```json
{
    "fee": 12,
    "amount": 234134,
    "script": "<script source>"
}
```