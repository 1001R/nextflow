package nextflow.util

import groovy.transform.TupleConstructor

@TupleConstructor(includeSuperProperties=true)
class AwsSessionCredentials extends AwsUserCredentials {
    String sessionToken
}
