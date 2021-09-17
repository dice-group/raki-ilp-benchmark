#!/usr/bin/env bash

source /opt/conda/etc/profile.d/conda.sh
conda activate ontolearn_env && simple_drill_endpoint --path_knowledge_base $1 --max_test_time_per_concept $4 --path_knowledge_base_embeddings $2 --pretrained_drill_avg_path $3
