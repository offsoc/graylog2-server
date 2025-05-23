<#if _title>Indexer node reaches maximum number of shards</#if>

<#if _description><span>
    Indexer node ${node} is using ${shards} shards.
    This is more than 90% of the maximum number of shards (${max_shards}) configured per node in the cluster.
    Please increase the maximum number of shards allowed or delete some indices to reduce the number of shards.
</span></#if>
