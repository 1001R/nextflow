package nextflow.util


import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

@EqualsAndHashCode(includes=['accessKeyId'])
@TupleConstructor
class AwsUserCredentials {
    String accessKeyId
    String secretAccessKey

    def asType(Class clazz) {
        if (clazz == List.class) {
            return [accessKeyId, secretAccessKey]
        }
    }
}
