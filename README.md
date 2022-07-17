# springboot-reactive
- Reactive Example.
- Calling multiple api parallel example.
- junit 5.
- Integration Test.
- helm : 
> Example and Guides to store Charts in repository : [Guide](https://helm.sh/docs/topics/chart_repository/#github-pages-example), [Example](https://github.com/technosophos/tscharts)
> Example and guide to store Charts in OCI : [Helm Registry](https://helm.sh/docs/topics/registries/#commands-for-working-with-registries), Below example for more details.

```bash
helm package --destination helm/charts --version 1.0.3 --app-version 1.0.4 helm/
helm registry login ghcr.io/samitkumarpatel --username samitkumarpatel
helm push helm/charts/helm-1.0.0.tgz oci://ghcr.io/samitkumarpatel/springboot-reactive

helm pull oci://ghcr.io/samitkumarpatel/springboot-reactive/helm --version 1.0.0
helm show all oci://ghcr.io/samitkumarpatel/springboot-reactive/helm --version 1.0.0
helm template springboot-reactive oci://ghcr.io/samitkumarpatel/springboot-reactive/helm --version 1.0.0
helm install springboot-reactive oci://ghcr.io/samitkumarpatel/springboot-reactive/helm --version 1.0.0
helm upgrade springboot-reactive --install --dry-run oci://ghcr.io/samitkumarpatel/springboot-reactive/helm --version 1.0.0
```
- ...

