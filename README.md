# elastic-tunnel

Tools for downloading data from elasticsearch server.

## Usage

```
usage: elastic-tunnel -h <arg> [-u <arg>] [-p <arg>] -i <arg> -f <arg> 
       [-l <arg>] -o <arg> [--pretty] [--scroll <arg>] [--size <arg>] 
    -h, --host <arg>     Elasticsearch host url
    -u, --user <arg>     Elasticsearch user name
    -p, --pass <arg>     Elasticsearch user password
    -i, --index <arg>    Elasticsearch index name or alias
    -f, --fields <arg>   The source fields to download
    -l, --limit <arg>    Max entries to download
    -o, --output <arg>   Output format [JSON|CSV]
        --pretty         Pretty printing
        --scroll <arg>   Elasticsearch scroll timeout
        --size <arg>     Elasticsearch scroll size
```

You need to enter a query body or redirected it from file, eg:

```
java -jar elastic-tunnel.jar -h http://127.0.0.1:9200 -i twitter --fields=id,user -o csv < query.json
```

`query.json` may like this:

```bash
{
  "term": {
    "country": "China"
  }
}
```

## [License](LICENSE.txt)

The MIT License (MIT)

Copyright (c) 2018 Wenxuan Zhang
