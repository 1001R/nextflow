package nextflow.util

import groovy.transform.TupleConstructor

@TupleConstructor(includeSuperProperties=true)
class AwsSessionCredentials extends AwsUserCredentials {
    String sessionToken

    def asType(Class clazz) {
        if (clazz == List.class) {
            return [accessKeyId, secretAccessKey, sessionToken]
        }
    }
}
