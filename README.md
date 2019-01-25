# elastic-tunnel

Tools for downloading data from elasticsearch cluster.

## Usage

```
usage: elastic-tunnel -i <index> -q <query> -o <format> [...]
     -f,--fields <arg>   Fields to download
     -h,--host <arg>     Elasticsearch host url
     -i,--index <arg>    Elasticsearch index name
     -l,--limit <arg>    Max entries to download
     -o,--output <arg>   Output format [JSON|CSV]
     -p,--pass <arg>     Elasticsearch user password
        --pretty         Pretty printing
     -q,--query <arg>    Query file
     -s,--slice <arg>    Elasticsearch Scroll slice
        --scroll <arg>   Elasticsearch scroll timeout
        --size <arg>     Elasticsearch scroll size
     -u,--user <arg>     Elasticsearch user name
```

## Example

```
elastic-tunnel -i twitter -f id,user -q query.json -o json --pretty
```

`query.json` may like this:

```bash
{
  "term": {
    "country": "China"
  }
}
```

## License

The MIT License ([MIT](https://wfxr.mit-license.org/2019))

Copyright (c) Wenxuan Zhang
