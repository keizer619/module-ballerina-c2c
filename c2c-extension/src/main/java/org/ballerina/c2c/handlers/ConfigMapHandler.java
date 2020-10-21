/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerina.c2c.handlers;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import org.ballerina.c2c.exceptions.KubernetesPluginException;
import org.ballerina.c2c.models.ConfigMapModel;
import org.ballerina.c2c.models.DeploymentModel;
import org.ballerina.c2c.utils.KubernetesUtils;

import java.io.IOException;
import java.util.Collection;

import static org.ballerina.c2c.KubernetesConstants.CONFIG_MAP_FILE_POSTFIX;
import static org.ballerina.c2c.KubernetesConstants.YAML;
import static org.ballerina.c2c.utils.KubernetesUtils.isBlank;

/**
 * Generates kubernetes Config Map.
 */
public class ConfigMapHandler extends AbstractArtifactHandler {

    private void generate(ConfigMapModel configMapModel) throws KubernetesPluginException {
        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(configMapModel.getName())
                .withNamespace(dataHolder.getNamespace())
                .endMetadata()
                .withData(configMapModel.getData())
                .build();
        try {
            String configMapContent = SerializationUtils.dumpWithoutRuntimeStateAsYaml(configMap);
            KubernetesUtils.writeToFile(configMapContent, CONFIG_MAP_FILE_POSTFIX + YAML);
        } catch (IOException e) {
            String errorMessage = "Error while parsing yaml file for config map: " + configMapModel.getName();
            throw new KubernetesPluginException(errorMessage, e);
        }
    }

    @Override
    public void createArtifacts() throws KubernetesPluginException {
        //configMap
        int count = 0;
        Collection<ConfigMapModel> configMapModels = dataHolder.getConfigMapModelSet();
        if (configMapModels.size() > 0) {
            OUT.println();
        }
        for (ConfigMapModel configMapModel : configMapModels) {
            count++;
            if (!isBlank(configMapModel.getBallerinaConf())) {
                if (configMapModel.getData().size() != 1) {
                    throw new KubernetesPluginException("there can be only 1 ballerina config file");
                }
                DeploymentModel deploymentModel = dataHolder.getDeploymentModel();
                deploymentModel.setCommandArgs(" --b7a.config.file=${CONFIG_FILE}");
//                EnvVarValueModel envVarValueModel = new EnvVarValueModel(configMapModel.getMountPath() +
//                        BALLERINA_CONF_FILE_NAME);
//                deploymentModel.addEnv("CONFIG_FILE", envVarValueModel);
                dataHolder.setDeploymentModel(deploymentModel);
            }
            generate(configMapModel);
            OUT.print("\t@kubernetes:ConfigMap \t\t\t - complete " + count + "/" + configMapModels.size() + "\r");
        }
    }
}