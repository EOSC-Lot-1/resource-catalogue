# README

## 1.Prepare

Cope `.env.example` to `.env` and edit accordingly.

Download database init scripts:

    docker-compose run --rm -T download_postgres_init_scripts    

Prepare secrets under `secrets` dir, using the following structure:

```
secrets/
└── pid
    └── clients
        ├── ifg
        │   ├── client.crt 
        │   └── client.key 
        ├── providers
        │   ├── client.crt 
        │   └── client.key 
        ├── services
        │   ├── client.crt 
        │   └── client.key 
        ├── tools
        │   ├── client.crt 
        │   └── client.key 
        └── training
            ├── client.crt 
            └── client.key 
```

## 2. Bring up

Start base services (and wait for postgres to be fully initialized from scripts):

    docker-compose up -d redis postgres

For each catalogue instance (`{services,tools,training,ifg}-catalogue`), apply db migrations and start the service. For example, for `services-catalogue`:

    docker-compose run --rm -T services-catalogue-db-migration
    docker-compose up -d services-catalogue
