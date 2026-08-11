# `k8s/config/`

Two ConfigMaps feed the cluster's configuration; only one of them is a file in this repo.

## `env-configmap.yaml` (committed, static)

Shared non-secret env vars (`CONFIG_SERVER_HOST`, `EUREKA_URL`, `DB_HOST`, `KAFKA_BOOTSTRAP_SERVERS`, …) — small, rarely-changing, and safe to hand-maintain as YAML. Applied normally: `kubectl apply -f k8s/config/env-configmap.yaml`.

## `cake-delight-config-repo` (generated, not committed)

This is `config-server`'s actual config source — `config-repo/*.properties`, unchanged — mounted as a ConfigMap volume instead of the bind mount Docker Compose uses. It is deliberately **not** hand-copied into a YAML manifest here: `config-repo/` is already the single source of truth for every service's configuration (DECISIONS.md D-12/D-16), and a second, YAML-embedded copy would drift from it the moment either one is edited without the other.

Generate it fresh from the real directory before deploying (from the repo root):

```sh
kubectl create namespace cake-delight --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap cake-delight-config-repo \
  --from-file=config-repo/ \
  -n cake-delight
```

Re-run the `create configmap` command (or `kubectl delete configmap cake-delight-config-repo -n cake-delight` first) any time a file under `config-repo/` changes and you want the cluster to pick it up — same "edit the file, redeploy" loop as editing it locally and restarting `config-server`, just one extra command since a ConfigMap isn't a live bind mount.
