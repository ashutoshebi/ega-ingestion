chmod g+w */target/*.jar
rsync --perms */target/*.jar hh-vault-01-01.ebi.ac.uk:/nfs/ega/private/ingestion_pipeline/dev
