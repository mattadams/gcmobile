{application, couch, [
    {description, "Apache CouchDB"},
    {vsn, "1.2.0a-573eba5-git"},
    {modules, [couch_native_process,couch_api_wrap,couch_log,couch_doc,couch_httpd_auth,couch_httpd_replicator,couch_db,couch_ejson_compare,couch_replication_manager,couch_db_updater,couch_view_updater,couch_config_writer,couch_httpd_db,couch_internal_load_gen,couch_indexer_manager,couch_server_sup,couch_httpd_show,couch_os_daemons,couch_uuids,couch_httpd,couch_app,couch_compress_types,couch_skew,couch_secondary_sup,jninif,couch_util,couch_httpd_oauth,couch,couch_httpd_proxy,couch_query_servers,couch_view,couch_auth_cache,couch_server,couch_db_frontend,couch_db_update_notifier,couch_api_wrap_httpc,couch_db_update_notifier_sup,couch_ref_counter,couch_primary_sup,couch_event_sup,couch_key_tree,couch_btree_copy,couch_view_group,json_stream_parse,couch_btree,couch_rep_sup,couch_access_log,couch_httpd_view,couch_view_merger_queue,couch_view_compactor,couch_replication_notifier,couch_httpd_stats_handlers,couch_external_server,couch_compress,couch_view_merger,couch_drv,couch_stream,couch_changes,couch_replicator_utils,couch_file,couch_work_queue,couch_replicator_worker,couch_httpc_pool,couch_os_process,couch_compaction_daemon,couch_config,couch_httpd_misc_handlers,couch_httpd_vhost,couch_httpd_external,couch_httpd_view_merger,couch_external_manager,couch_httpd_rewrite,couch_replicator,couch_stats_aggregator,couch_stats_collector,couch_task_status]},
    {registered, [
        couch_config,
        couch_db_update,
        couch_db_update_notifier_sup,
        couch_external_manager,
        couch_httpd,
        couch_log,
        couch_access_log,
        couch_primary_services,
        couch_query_servers,
        couch_rep_sup,
        couch_secondary_services,
        couch_server,
        couch_server_sup,
        couch_stats_aggregator,
        couch_stats_collector,
        couch_task_status,
        couch_view
    ]},
    {mod, {couch_app, [
        "/data/data/%app_name%/couchdb/etc/couchdb/default.ini",
        "/data/data/%app_name%/couchdb/etc/couchdb/local.ini"
    ]}},
    {applications, [kernel, stdlib]},
    {included_applications, [crypto, sasl, ibrowse, mochiweb, os_mon]}
]}.
