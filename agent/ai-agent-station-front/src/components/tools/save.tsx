import { useState, useEffect, useCallback } from 'react';

import { useClientContext, getNodeForm, FlowNodeEntity } from '@flowgram.ai/free-layout-editor';
import { Button, Badge, Toast } from '@douyinfe/semi-ui';
import { API_ENDPOINTS, DEFAULT_HEADERS } from '../../config/api';
import { getUrlParam } from '../../utils/url';

export function Save(props: { disabled: boolean }) {
  const DEFAULT_AGENT_ID = '1';
  const [errorCount, setErrorCount] = useState(0);
  const clientContext = useClientContext();

  const updateValidateData = useCallback(() => {
    const allForms = clientContext.document.getAllNodes().map((node) => getNodeForm(node));
    const count = allForms.filter((form) => form?.state.invalid).length;
    setErrorCount(count);
  }, [clientContext]);

  /**
   * 调用后端API保存流程图配置
   */
  const saveToBackend = async (configData: any) => {
    try {
      const getAgentInfoFromConfig = (config: any) => {
        let name = `流程图配置_${new Date().toLocaleString()}`;
        let desc = '通过前端拖拽生成的流程图配置';
        if (config?.nodes && Array.isArray(config.nodes)) {
          const agentNode = config.nodes.find(
            (n: any) => n?.type === 'agent' && n?.data && n?.data.inputsValues
          );
          if (agentNode) {
            const inputsValues = agentNode.data.inputsValues;
            const agentNameVal = inputsValues?.agentName;
            if (Array.isArray(agentNameVal) && agentNameVal.length > 0) {
              const first = agentNameVal[0];
              if (typeof first === 'string') name = first;
              else if (typeof first?.value === 'string') name = first.value;
              else if (typeof first?.value?.content === 'string') name = first.value.content;
              else if (typeof first?.content === 'string') name = first.content;
            } else if (typeof agentNameVal === 'string') {
              name = agentNameVal;
            }

            const descVal = inputsValues?.description;
            if (Array.isArray(descVal) && descVal.length > 0) {
              const first2 = descVal[0];
              if (typeof first2 === 'string') desc = first2;
              else if (typeof first2?.value === 'string') desc = first2.value;
              else if (typeof first2?.value?.content === 'string') desc = first2.value.content;
              else if (typeof first2?.content === 'string') desc = first2.content;
            } else if (typeof descVal === 'string') {
              desc = descVal;
            }
          }
        }
        return { name, desc };
      };

      const { name: configName, desc: description } = getAgentInfoFromConfig(configData);
      const configId = getUrlParam('configId') ?? undefined;
      const requestData: Record<string, unknown> = {
        configName,
        description,
        configData: JSON.stringify(configData),
        createBy: 'system',
        updateBy: 'system'
      };
      if (configId) requestData.configId = configId;
      requestData.agentId = DEFAULT_AGENT_ID;

      const response = await fetch(`${API_ENDPOINTS.AI_AGENT_DRAW.BASE}${API_ENDPOINTS.AI_AGENT_DRAW.SAVE_CONFIG}`, {
        method: 'POST',
        headers: DEFAULT_HEADERS,
        body: JSON.stringify(requestData),
      });

      const result = await response.json();

      if (result.code === '0000') {
        Toast.success(`保存成功！配置ID: ${result.data}`);
        return result.data;
      } else {
        Toast.error(`保存失败: ${result.info}`);
        throw new Error(result.info);
      }
    } catch (error) {
      console.error('保存流程图配置失败:', error);
      Toast.error('保存失败，请检查网络连接');
      throw error;
    }
  };

  /**
   * 后端要求每条边包含 source、target（或等价 sourceNodeId/targetNodeId）。
   * 画布 toJSON 可能只有 sourceNodeID/targetNodeID，在此统一补全 source、target。
   */
  const normalizeEdgesForBackend = (configData: any) => {
    if (!configData?.edges?.length) return configData;
    configData.edges.forEach((edge: any) => {
      if (edge.source == null && edge.sourceNodeID != null) edge.source = edge.sourceNodeID;
      if (edge.target == null && edge.targetNodeID != null) edge.target = edge.targetNodeID;
    });
    return configData;
  };

  /**
   * 后端读取 agentName 时希望 firstItem.value 直接是字符串。
   * 将 value.content 扁平化为 value:string；若空则使用 agentId 兜底。
   */
  const normalizeAgentNameForBackend = (configData: any) => {
    if (!configData?.nodes?.length) return configData;
    configData.nodes.forEach((node: any) => {
      if (node?.type !== 'agent' || !node?.data?.inputsValues) return;
      const { inputsValues } = node.data;
      const agentName = inputsValues.agentName;
      if (Array.isArray(agentName)) {
        agentName.forEach((item: any) => {
          if (!item || typeof item !== 'object') return;
          if (typeof item.value === 'string' && item.value.trim() !== '') return;
          if (typeof item.value?.content === 'string' && item.value.content.trim() !== '') {
            item.value = item.value.content;
            return;
          }
          if (typeof item.content === 'string' && item.content.trim() !== '') {
            item.value = item.content;
            return;
          }
          item.value = DEFAULT_AGENT_ID;
        });
        return;
      }
      if (typeof agentName?.value?.content === 'string' && agentName.value.content.trim() !== '') {
        inputsValues.agentName = agentName.value.content;
        return;
      }
      if (typeof agentName?.content === 'string' && agentName.content.trim() !== '') {
        inputsValues.agentName = agentName.content;
        return;
      }
      if (typeof agentName !== 'string' || agentName.trim() === '') {
        inputsValues.agentName = DEFAULT_AGENT_ID;
      }
    });
    return configData;
  };

  /**
   * 发送前最后一步：保证每个节点的 refId 都有值，避免后端 DrawConfigParser 校验失败并打 WARN。
   * 若 refId 不存在、为 null 或为空字符串，则用节点 id 补全。
   * 同时写入 node.refId 与 node.data.refId，兼容后端从不同路径读取的情况。
   */
  const ensureRefIdForAllNodes = (configData: any) => {
    if (!configData?.nodes?.length) return configData;
    configData.nodes.forEach((node: any) => {
      const current = node.refId ?? node.data?.refId;
      const validRefId =
        current != null && String(current).trim() !== ''
          ? String(current).trim()
          : node.id;
      const needWarn = current == null || String(current).trim() === '';
      node.refId = validRefId;
      if (node.data == null) node.data = {};
      node.data.refId = validRefId;
      if (needWarn) console.warn(`节点 ${node.id} 缺少 refId，已自动补全为 ${validRefId}。`);
    });
    return configData;
  };

  /**
   * 提取客户端信息并添加到配置数据中
   */
  const extractClientInfo = (configData: any) => {
    normalizeAgentNameForBackend(configData);
    ensureRefIdForAllNodes(configData);
    normalizeEdgesForBackend(configData);

    // 遍历所有节点，查找client类型的节点
    if (configData.nodes) {
      configData.nodes.forEach((node: any) => {
        if (node.type === 'client' && node.data && node.data.inputsValues) {
          const inputsValues = node.data.inputsValues;

          // clientName现在是字符串格式，clientId应该已经在组件中设置到inputsValues中
          if (inputsValues.clientName && typeof inputsValues.clientName === 'string') {
            // 如果clientId已经存在，保持不变；如果不存在，尝试从clientName推断
            if (!inputsValues.clientId) {
              console.warn('clientId为空，可能是选择组件没有正确设置clientId');
              // 这里可以添加降级处理逻辑，比如使用clientName作为临时ID
              inputsValues.clientId = '';
            }
            console.info('Client info:', {
              clientName: inputsValues.clientName,
              clientId: inputsValues.clientId
            });
          }
        }
      });
    }

    return configData;
  };

  /**
   * Validate all node and Save
   */
  const onSave = useCallback(async () => {
    try {
      const allForms = clientContext.document.getAllNodes().map((node) => getNodeForm(node));
      await Promise.all(allForms.map(async (form) => form?.validate()));

      const originalConfigData = clientContext.document.toJSON();

      // 提取客户端信息并添加到配置数据中
      const configData = extractClientInfo(originalConfigData);

      console.log('>>>>> save data: ', configData);

      // 调用后端API保存配置
      await saveToBackend(configData);
    } catch (error) {
      console.error('保存过程中发生错误:', error);
    }
  }, [clientContext]);

  /**
   * Listen single node validate
   */
  useEffect(() => {
    const listenSingleNodeValidate = (node: FlowNodeEntity) => {
      const form = getNodeForm(node);
      if (form) {
        const formValidateDispose = form.onValidate(() => updateValidateData());
        node.onDispose(() => formValidateDispose.dispose());
      }
    };
    clientContext.document.getAllNodes().map((node) => listenSingleNodeValidate(node));
    const dispose = clientContext.document.onNodeCreate(({ node }) =>
      listenSingleNodeValidate(node)
    );
    return () => dispose.dispose();
  }, [clientContext]);

  if (errorCount === 0) {
    return (
      <Button
        disabled={props.disabled}
        onClick={onSave}
        style={{ backgroundColor: 'rgba(171,181,255,0.3)', borderRadius: '8px' }}
      >
        Save
      </Button>
    );
  }
  return (
    <Badge count={errorCount} position="rightTop" type="danger">
      <Button
        type="danger"
        disabled={props.disabled}
        onClick={onSave}
        style={{ backgroundColor: 'rgba(255, 179, 171, 0.3)', borderRadius: '8px' }}
      >
        Save
      </Button>
    </Badge>
  );
}
