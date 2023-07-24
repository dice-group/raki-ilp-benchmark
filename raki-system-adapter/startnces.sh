#!/usr/bin/env bash

source /opt/conda/etc/profile.d/conda.sh
conda activate ontolearn_env && nces_endpoint --path_knowledge_base $1 --path_knowledge_base_embeddings $2
