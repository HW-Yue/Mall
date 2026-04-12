import { useEffect, useState } from 'react';

import { Field } from '@flowgram.ai/free-layout-editor';
import { Select } from '@douyinfe/semi-ui';

import { AiClientService } from '../../../services';
import type { AiClientResponseDTO } from '../../../services/ai-client-service';
import { useIsSidebar, useNodeRenderContext, useAgentStrategy } from '../../../hooks';
import { FormItem } from '../../../form-components';
import { ClientPort } from './styles';
import type { ClientTypeValue } from '../client-types';

interface ClientInstanceSelectProps {
  selectedClientType: string;
  hasStrategy: boolean;
  readonly: boolean;
  nodeRender: ReturnType<typeof useNodeRenderContext>;
}

function ClientInstanceSelect({
  selectedClientType,
  hasStrategy,
  readonly,
  nodeRender,
}: ClientInstanceSelectProps) {
  const [instanceOptions, setInstanceOptions] = useState<{ label: string; value: string }[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const hasClientType = Boolean(selectedClientType);

  useEffect(() => {
    if (!hasStrategy || !hasClientType) {
      setInstanceOptions([]);
      return;
    }
    setLoading(true);
    setError(false);
    AiClientService.queryClientsByClientType(selectedClientType)
      .then((clients: AiClientResponseDTO[]) => {
        setInstanceOptions(clients.map((c) => ({ label: c.clientName, value: c.clientId })));
      })
      .catch((err) => {
        console.error('获取客户端实例失败:', err);
        setError(true);
        setInstanceOptions([]);
      })
      .finally(() => setLoading(false));
  }, [hasStrategy, selectedClientType, hasClientType]);

  const step2Disabled = readonly || !hasStrategy || !hasClientType || loading;
  const placeholder = !hasStrategy
    ? '请先在 Agent 节点选择策略'
    : !hasClientType
      ? '请先选择客户端类型（槽位）'
      : loading
        ? '加载实例列表中...'
        : error
          ? '加载失败，请刷新重试'
          : instanceOptions.length === 0
            ? '该类型下暂无可用实例'
            : '请选择客户端实例';

  return (
    <Field<string> name="inputsValues.clientName">
      {({ field: clientNameField }) => (
        <Field<string> name="inputsValues.clientId">
          {({ field: clientIdField }) => {
            const nodeInputsValues = nodeRender.node.getNodeMeta()?.inputsValues as Record<string, unknown> | undefined;
            let displayValue = (clientIdField.value as string) ?? '';
            let displayLabel = (clientNameField.value as string) ?? '';

            if (displayValue && instanceOptions.length) {
              const matched = instanceOptions.find((o) => o.value === displayValue);
              if (matched) displayLabel = matched.label;
            } else if (displayLabel && instanceOptions.length) {
              const matched = instanceOptions.find((o) => o.label === displayLabel);
              if (matched) displayValue = matched.value;
            } else if (nodeInputsValues) {
              const nid = nodeInputsValues.clientId as string | undefined;
              const nname = nodeInputsValues.clientName as string | undefined;
              if (nid && instanceOptions.some((o) => o.value === nid)) {
                displayValue = nid;
                const m = instanceOptions.find((o) => o.value === nid);
                if (m) {
                  displayLabel = m.label;
                  if (!clientIdField.value) clientIdField.onChange(displayValue);
                  if (!clientNameField.value) clientNameField.onChange(displayLabel);
                }
              } else if (nname && instanceOptions.some((o) => o.label === nname)) {
                displayLabel = nname;
                const m = instanceOptions.find((o) => o.label === nname);
                if (m) {
                  displayValue = m.value;
                  if (!clientIdField.value) clientIdField.onChange(displayValue);
                  if (!clientNameField.value) clientNameField.onChange(displayLabel);
                }
              }
            }

            return (
              <FormItem name="客户端实例" type="string" required={true} labelWidth={100}>
                <Select
                  placeholder={placeholder}
                  style={{ width: '100%' }}
                  value={displayValue}
                  onChange={(value) => {
                    const selected = instanceOptions.find((o) => o.value === value);
                    clientNameField.onChange(selected?.label ?? '');
                    clientIdField.onChange(selected?.value ?? '');
                  }}
                  disabled={step2Disabled}
                  optionList={instanceOptions}
                  loading={loading}
                />
                {readonly && (displayLabel || displayValue) && (
                  <div style={{ marginTop: '4px', fontSize: '12px', color: '#666' }}>
                    {displayLabel && <div>客户端名称: {displayLabel}</div>}
                    {displayValue && <div>客户端ID: {displayValue}</div>}
                  </div>
                )}
                <ClientPort data-port-id="client-output" data-port-type="output" />
              </FormItem>
            );
          }}
        </Field>
      )}
    </Field>
  );
}

export function ClientSelect() {
  const readonly = !useIsSidebar();
  const nodeRender = useNodeRenderContext();
  const agentStrategy = useAgentStrategy();
  const hasStrategy = Boolean(agentStrategy?.trim());

  return (
    <Field<ClientTypeValue> name="inputsValues.clientType.0">
      {({ field }) => {
        const selectedClientType =
          field.value?.value ?? (typeof field.value === 'string' ? field.value : '');
        return (
          <ClientInstanceSelect
            selectedClientType={selectedClientType}
            hasStrategy={hasStrategy}
            readonly={readonly}
            nodeRender={nodeRender}
          />
        );
      }}
    </Field>
  );
}
