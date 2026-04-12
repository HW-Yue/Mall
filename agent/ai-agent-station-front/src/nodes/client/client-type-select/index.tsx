import { useEffect, useState, useMemo } from 'react';

import { Field } from '@flowgram.ai/free-layout-editor';
import { Select } from '@douyinfe/semi-ui';

import { useIsSidebar, useAgentStrategy } from '../../../hooks';
import { FormItem } from '../../../form-components';
import { AiAgentDrawService } from '../../../services';
import { CLIENT_TYPE_OPTIONS, ClientTypeValue, ClientConfigType } from '../client-types';

/** 根据策略返回的 client_type 编码列表，映射为 Select 的 optionList（label + value） */
function mapClientTypeCodesToOptions(codes: string[]): { label: string; value: string }[] {
  const codeSet = new Set(codes);
  return CLIENT_TYPE_OPTIONS.filter((opt) => codeSet.has(opt.value)).map((opt) => ({
    label: opt.label,
    value: opt.value,
  }));
}

export function ClientTypeSelect() {
  const readonly = !useIsSidebar();
  const agentStrategy = useAgentStrategy();
  const [slotOptions, setSlotOptions] = useState<{ label: string; value: string }[]>([]);
  const [loading, setLoading] = useState(false);

  const hasStrategy = Boolean(agentStrategy?.trim());

  useEffect(() => {
    if (!hasStrategy) {
      setSlotOptions([]);
      return;
    }
    setLoading(true);
    AiAgentDrawService.getClientTypesByStrategy(agentStrategy)
      .then((codes) => setSlotOptions(mapClientTypeCodesToOptions(codes)))
      .catch(() => setSlotOptions([]))
      .finally(() => setLoading(false));
  }, [agentStrategy, hasStrategy]);

  return (
    <Field<ClientTypeValue> name="inputsValues.clientType.0">
      {({ field, fieldState }) => {
        let displayValue = field.value?.value ?? '';

        if (field.value) {
          if (typeof field.value === 'object' && field.value.value) {
            displayValue = field.value.value;
          } else if (typeof field.value === 'string') {
            displayValue = field.value as ClientConfigType;
          }
        }

        // 若当前值不在当前策略的槽位列表中，仅展示上不显示（不在此处清空，避免渲染中 setState）
        const validValue =
          slotOptions.length > 0 && displayValue && slotOptions.some((o) => o.value === displayValue)
            ? displayValue
            : '';

        const disabled = readonly || !hasStrategy || loading;
        const placeholder = !hasStrategy
          ? '请先在 Agent 节点选择策略'
          : loading
            ? '加载槽位列表中...'
            : slotOptions.length === 0
              ? '该策略暂无可用槽位'
              : '请选择客户端类型（槽位）';

        return (
          <FormItem name="客户端类型（槽位）" type="string" required={true} labelWidth={120}>
            <Select
              placeholder={placeholder}
              style={{ width: '100%' }}
              value={validValue}
              onChange={(value) =>
                field.onChange({
                  key: field.value?.key || `client_type_${Date.now()}`,
                  value: (value as ClientConfigType) ?? '',
                })
              }
              disabled={disabled}
              optionList={slotOptions}
              loading={loading}
            />
          </FormItem>
        );
      }}
    </Field>
  );
}
