# CreateFunction/UpdateFunctionCode reject a handler provided by a layer

## Summary

The handler-file check that runs after the deployment package is extracted only walks the function's own code directory and ignores the function's layers, so any function whose handler module ships in a layer is rejected:

```
An error occurred (InvalidParameterValueException) when calling the CreateFunction operation: Handler file 'entrypoint' not found in deployment package
```

Real AWS Lambda does not check handler presence when the function is created (a genuinely missing handler surfaces at invocation time as `Runtime.ImportModuleError`), and a handler module shipped in a layer is a supported layout: layer contents are extracted to `/opt`, and for Python runtimes `/opt/python` is on `sys.path`. We run exactly this layout in production on real AWS — a shared entrypoint module lives in a common layer and each function's deployment package carries only its own code.

floci's own runtime handles it correctly (`ContainerLauncher` copies the resolved layers into `/opt` and the module is imported from there); only the pre-flight check rejects it.

## Environment

- floci `2.0.1-main-d2d7eb41` (JVM image) and `1.7.0` (native image), community edition, run as a container with the docker socket mounted. Also present in `main` at `2.0.1-322-g47d96f61`.
- Runtime: `python3.12`. Client: aws-cli v2.

## Steps to reproduce

```bash
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=eu-west-1
E="--endpoint-url=http://127.0.0.1:4566"

# a layer that provides the handler module
mkdir -p layer/python
cat > layer/python/entrypoint.py <<'EOF'
def lambda_handler(event, context):
  return {'ok': True, 'from': 'layer'}
EOF
(cd layer && zip -qr ../layer.zip python)

# the function's deployment package does NOT contain entrypoint.py
mkdir -p fn && echo "print('business code')" > fn/business.py
(cd fn && zip -q ../fn.zip business.py)

aws $E iam create-role --role-name repro --assume-role-policy-document \
  '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"lambda.amazonaws.com"},"Action":"sts:AssumeRole"}]}'

LAYER=$(aws $E lambda publish-layer-version --layer-name entrypoint-layer \
  --zip-file fileb://layer.zip --compatible-runtimes python3.12 \
  --query LayerVersionArn --output text)

aws $E lambda create-function --function-name repro --runtime python3.12 \
  --role arn:aws:iam::000000000000:role/repro \
  --handler entrypoint.lambda_handler \
  --zip-file fileb://fn.zip --layers "$LAYER"
```

## Actual behaviour

`CreateFunction` fails with `InvalidParameterValueException: Handler file 'entrypoint' not found in deployment package`. Same result when the code comes from S3 (`--code S3Bucket=…,S3Key=…`) instead of `--zip-file`, and the same on `UpdateFunctionCode`.

## Expected behaviour

The function is created, as it is on real AWS. If the pre-flight check is worth keeping, it should also consider the resolved layers before failing (for Python: `python/<module>.py` and `python/lib/python*/site-packages/<module>.py` inside each layer, mirroring what ends up on `sys.path`), or be downgraded to a warning.

## Evidence that floci's runtime resolves it fine

`UpdateFunctionConfiguration` does not run the check, so creating the function with a handler that *is* in the package and then repointing it at the layer's module yields a working function — i.e. the rejected configuration is one floci executes correctly:

```bash
# same fn.zip (no entrypoint.py), handler initially inside the package
aws $E lambda create-function --function-name repro-update --runtime python3.12 \
  --role arn:aws:iam::000000000000:role/repro --handler business.lambda_handler \
  --zip-file fileb://fn.zip --layers "$LAYER"

aws $E lambda update-function-configuration --function-name repro-update \
  --handler entrypoint.lambda_handler

aws $E lambda invoke --function-name repro-update --payload '{}' out.json
cat out.json   # {"ok": true, "from": "layer"}
```

`StatusCode: 200` and `{"ok": true, "from": "layer"}`: the module is imported from the layer, exactly as on AWS. Note the inconsistency — the same final state is reachable through a call that skips the check. That two-step dance is also the only workaround short of duplicating the handler module into every deployment package, and it does not survive a code update: `UpdateFunctionCode` on that very function fails with the same error while the function keeps working.

## Cause

In `LambdaService.extractZipCodeBytes`, right after the package is extracted:

- `resolveHandlerFilePath(fn)` turns the handler into a relative module path.
- `Files.walk(codePath)` then looks for that path **only** under the function's extracted code directory, and throws `InvalidParameterValueException` when it is not there. Layers are never consulted, even though `fn.getLayers()` is available and `LambdaLayerService.resolveLayerByArn` already resolves them elsewhere.

The check therefore fires on every path that extracts a package: `createFunction`, `updateFunctionCode` and the S3 hot-reload in `onS3ObjectUpdated`. `updateFunctionConfiguration` is the only one that changes the handler without going through it.

For contrast, `ContainerLauncher` (step *"Copy layer contents into /opt (layers are merged in order)"*) resolves each layer ARN via `layerService.resolveLayerByArn` and copies its content into `/opt` — the same resolution the check would need.

A comment in `CloudFormationLambdaMissingS3CodeIntegrationTest` already calls this message misleading in another scenario, which suggests the check surfaces as a confusing error in more than one case.

## Impact

Every function whose handler lives in a shared layer must be created with a throwaway handler and patched afterwards — and then it cannot receive code updates through `UpdateFunctionCode` — or the handler module has to be duplicated into each deployment package. When the deployment package is produced by tooling that also targets real AWS, that means altering the artifacts just for floci.
