package nextflow.cloud.aws

import com.amazonaws.auth.*
import nextflow.util.AwsProcessCredentials
import nextflow.util.AwsSessionCredentials
import nextflow.util.AwsUserCredentials

class CredentialsProviderFactory {
    public static CredentialsProviderFactory INSTANCE = new CredentialsProviderFactory()
    private Map credentialProviders = [:].withDefault { doCreateCredentialsProvider(it) }

    private CredentialsProviderFactory() {

    }

    synchronized AWSCredentialsProvider createCredentialsProvider(Object credentials) {
        return credentials ? credentialProviders[credentials] : DefaultAWSCredentialsProviderChain.getInstance()
    }

    private static AWSCredentialsProvider doCreateCredentialsProvider(Object c) {
        switch (c) {
            case AwsSessionCredentials:
                AWSCredentials credentials = new BasicSessionCredentials(c.accessKeyId, c.secretAccessKey, c.sessionToken)
                return new AWSStaticCredentialsProvider(credentials)
            case AwsUserCredentials:
                AWSCredentials credentials = new BasicAWSCredentials(c.accessKeyId, c.secretAccessKey)
                return new AWSStaticCredentialsProvider(credentials)
            case AwsProcessCredentials:
                return ProcessCredentialsProvider.builder()
                        .withCommand(c.command)
                        .build()
        }
        return DefaultAWSCredentialsProviderChain.getInstance()
    }
}
