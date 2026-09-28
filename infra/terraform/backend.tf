# Phase 1.2: Remote backend — stores state in GCS so the whole team shares
# a single source of truth. Bucket must exist before `terraform init`;
# see README for the one-time `gsutil mb` command.
terraform {
  backend "gcs" {
    # Override with -backend-config="bucket=<YOUR_STATE_BUCKET>" at init time,
    # or set GOOGLE_BACKEND_BUCKET env var. The bucket name is intentionally
    # left as a placeholder so this file is safe to commit.
    bucket = "REPLACE_WITH_YOUR_STATE_BUCKET"
    prefix = "nutrihealth/terraform/state"
  }
}
