PYTHONPATH=/proj/adsx/invenio26/lib/python:${common.dir}/build/dist:${common.dir}/src/python:${common.dir}/contrib/invenio/src/python
MONTYSOLR_HANDLER=montysolr.sequential_handler
MONTYSOLR_TARGETS=adslabs.targets,adslabs.schema.targets
MONTYSOLR_MAX_WORKERS=4
MONTYSOLR_JVMARGS=-d64 -Xmx2048m -Dmontysolr.max_workers=4 -Dmontysolr.max_threads=200 -Dsolr.cache.size=0 -Dsolr.cache.initial=0 -agentlib:jdwp=transport=dt_socket,server=y,address=8000
MONTYSOLR_ARGS=--daemon --port 8984