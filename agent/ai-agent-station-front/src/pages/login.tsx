import React, { useEffect, useRef, useState } from 'react';
import { Button, Form, Input, Toast, Typography } from '@douyinfe/semi-ui';
import { IconEyeClosed, IconEyeOpened, IconLock, IconSafe, IconUser } from '@douyinfe/semi-icons';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { theme } from '../styles/theme';
import { Card } from '../components/common';
import { AdminUserService, WechatLoginService } from '../services';

const { Title, Text } = Typography;

// 样式组件
const LoginContainer = styled.div`
  min-height: 100vh;
  background: ${theme.colors.bg.secondary};
  display: flex;
  align-items: center;
  justify-content: center;
  padding: ${theme.spacing.lg};
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: ${theme.colors.gradient.primary};
    opacity: 0.05;
    pointer-events: none;
  }
`;

const LoginWrapper = styled.div`
  width: 100%;
  max-width: 1200px;
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: ${theme.spacing['3xl']};
  align-items: center;
  
  @media (max-width: ${theme.breakpoints.lg}) {
    grid-template-columns: 1fr;
    max-width: 400px;
    gap: ${theme.spacing.xl};
  }
`;

const BrandSection = styled.div`
  text-align: center;
  
  @media (max-width: ${theme.breakpoints.lg}) {
    display: none;
  }
`;

const BrandLogo = styled.div`
  width: 120px;
  height: 120px;
  margin: 0 auto ${theme.spacing.xl};
  background: ${theme.colors.gradient.primary};
  border-radius: ${theme.borderRadius['2xl']};
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: ${theme.shadows.lg};
  
  .semi-icon {
    font-size: 48px;
    color: white;
  }
`;

const BrandTitle = styled(Title)`
  color: ${theme.colors.text.primary};
  margin-bottom: ${theme.spacing.base} !important;
  font-weight: ${theme.typography.fontWeight.bold};
`;

const BrandDescription = styled(Text)`
  color: ${theme.colors.text.secondary};
  font-size: ${theme.typography.fontSize.lg};
  line-height: ${theme.typography.lineHeight.relaxed};
`;

const LoginCard = styled(Card)`
  padding: ${theme.spacing['2xl']} !important;
  box-shadow: ${theme.shadows.xl} !important;
  border: none !important;
  min-height: 420px;
  display: flex;
  flex-direction: column;
  justify-content: center;
`;

const LoginHeader = styled.div`
  text-align: center;
  margin-bottom: ${theme.spacing.xl};
`;

const LoginTitle = styled(Title)`
  color: ${theme.colors.text.primary};
  margin-bottom: ${theme.spacing.sm} !important;
  font-weight: ${theme.typography.fontWeight.bold};
`;

const LoginSubtitle = styled(Text)`
  color: ${theme.colors.text.secondary};
  font-size: ${theme.typography.fontSize.base};
`;

const StyledForm = styled(Form)`
  .semi-form-field {
    margin-bottom: ${theme.spacing.lg};
  }
`;
styled(Input)`
  height: 48px;
  border-radius: ${theme.borderRadius.base};
  border: 1px solid ${theme.colors.border.primary};
  
  &:focus {
    border-color: ${theme.colors.primary};
    box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
  }
  
  .semi-input-prefix {
    color: ${theme.colors.text.tertiary};
  }
`;
const LoginButton = styled(Button)`
  width: 100%;
  height: 48px;
  border-radius: ${theme.borderRadius.base};
  background: ${theme.colors.gradient.primary} !important;
  border: none !important;
  color: white !important;
  font-weight: ${theme.typography.fontWeight.medium};
  font-size: ${theme.typography.fontSize.base};
  
  &:hover {
    opacity: 0.9;
    transform: translateY(-1px);
  }
`;

const QuickLoginButton = styled(Button)`
  width: 100%;
  height: 40px;
  border-radius: ${theme.borderRadius.base};
  border: 1px solid ${theme.colors.border.primary};
  color: ${theme.colors.text.secondary};
  background: ${theme.colors.bg.primary};
  
  &:hover {
    border-color: ${theme.colors.primary};
    color: ${theme.colors.primary};
  }
`;

const Divider = styled.div`
  display: flex;
  align-items: center;
  margin: ${theme.spacing.xl} 0;
  
  &::before,
  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: ${theme.colors.border.secondary};
  }
  
  span {
    padding: 0 ${theme.spacing.base};
    color: ${theme.colors.text.tertiary};
    font-size: ${theme.typography.fontSize.sm};
  }
`;

const WechatSection = styled.div`
  margin-top: ${theme.spacing.lg};
  padding-top: ${theme.spacing.lg};
  border-top: 1px dashed ${theme.colors.border.secondary};
`;

const WechatHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: ${theme.spacing.base};
`;

const WechatTitle = styled(Text)`
  color: ${theme.colors.text.primary};
  font-weight: ${theme.typography.fontWeight.medium};
`;

const WechatHint = styled(Text)`
  color: ${theme.colors.text.tertiary};
  font-size: ${theme.typography.fontSize.sm};
`;

const QrWrapper = styled.div`
  margin-top: ${theme.spacing.base};
  padding: ${theme.spacing.lg};
  border-radius: ${theme.borderRadius.lg};
  background: ${theme.colors.bg.secondary};
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: ${theme.spacing.lg};
`;

const QrImage = styled.div`
  width: 220px;
  height: 220px;
  border-radius: ${theme.borderRadius.lg};
  background: white;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid ${theme.colors.border.secondary};

  img {
    width: 200px;
    aspect-ratio: 1 / 1;
    object-fit: contain;
    border-radius: ${theme.borderRadius.base};
  }
`;

const QrDesc = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: ${theme.spacing.xs};
  text-align: center;
`;

const BindFormWrapper = styled.div`
  margin-top: ${theme.spacing.lg};
  padding: ${theme.spacing.lg};
  border-radius: ${theme.borderRadius.lg};
  background: ${theme.colors.bg.secondary};
`;

type WechatStep = 'idle' | 'qr' | 'binding';

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [isScanMode, setIsScanMode] = useState(false);
  const [wechatStep, setWechatStep] = useState<WechatStep>('idle');
  const [wechatTicket, setWechatTicket] = useState<string | null>(null);
  const [wechatQrUrl, setWechatQrUrl] = useState<string | null>(null);
  const [wechatLoading, setWechatLoading] = useState(false);
  const pollingRef = useRef<number | null>(null);

  const NEED_REGISTER_FLAG = '__need_register__';

  const clearPolling = () => {
    if (pollingRef.current !== null) {
      window.clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
  };

  useEffect(() => {
    return () => {
      clearPolling();
    };
  }, []);

  const setLoginStateByUserId = (userId: string) => {
    // 统一登录逻辑：写 cookie 供路由守卫使用，同时写入 localStorage 兼容业务页面
    const days = 30;
    const expires = new Date();
    expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
    document.cookie = `loginToken=${encodeURIComponent(userId)};expires=${expires.toUTCString()};path=/`;

    const now = new Date().toISOString();
    localStorage.setItem('token', userId);
    localStorage.setItem(
      'userInfo',
      JSON.stringify({
        username: userId,
        loginTime: now,
        token: userId,
      })
    );
    localStorage.setItem('isLoggedIn', 'true');
  };

  const handleLogin = async (values: Record<string, any>) => {
    setLoading(true);
    try {
      if (!values.username || !values.password) {
        Toast.error('请输入账号和密码');
        return;
      }

      // 调用后端登录校验接口
      const userId = await AdminUserService.validateAdminUserLogin({
        username: values.username,
        password: values.password
      });
      
      if (userId) {
        setLoginStateByUserId(userId);
        Toast.success('登录成功！');
        navigate('/dashboard');
      } else {
        Toast.error('账号或密码错误，请重试');
      }
    } catch (error) {
      console.error('登录失败:', error);
      Toast.error('登录失败，请检查网络连接或稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleTestLogin = () => {
    handleLogin({ username: 'admin', password: '123456' });
  };

  const handleGuestLogin = () => {
    const guestId = `guest_${Date.now()}`;
    setLoginStateByUserId(guestId);
    Toast.success('以游客身份进入');
    navigate('/dashboard');
  };

  const handleStartWechatLogin = async () => {
    setIsScanMode(true);
    setWechatLoading(true);
    clearPolling();
    try {
      const ticket = await WechatLoginService.getWeixinQrTicket();
      if (!ticket) {
        Toast.error('获取微信二维码失败，请稍后重试');
        return;
      }
      setWechatTicket(ticket);
      const qr = `https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=${encodeURIComponent(ticket)}`;
      setWechatQrUrl(qr);
      setWechatStep('qr');

      pollingRef.current = window.setInterval(async () => {
        const result = await WechatLoginService.checkLogin(ticket);
        if (!result || result.code !== '0000') {
          return;
        }
        // 登录流程结束，停止轮询
        clearPolling();

        if (result.data === NEED_REGISTER_FLAG) {
          setWechatStep('binding');
          Toast.info('检测到未绑定账号，请先完成注册绑定');
          return;
        }

        // 已有绑定账号，直接登录
        setLoginStateByUserId(result.data);
        Toast.success('微信登录成功');
        navigate('/dashboard');
      }, 3000);
    } catch (error) {
      console.error('微信登录初始化异常:', error);
      Toast.error('微信登录初始化失败，请稍后重试');
    } finally {
      setWechatLoading(false);
    }
  };

  const handleBindRegister = async (values: Record<string, any>) => {
    if (!wechatTicket) {
      Toast.error('参数异常，请重新扫码');
      setWechatStep('idle');
      return;
    }
    if (!values.bindUsername || !values.bindPassword) {
      Toast.error('请填写用户名和密码');
      return;
    }

    setWechatLoading(true);
    try {
      const result = await WechatLoginService.registerAndBind({
        ticket: wechatTicket,
        username: values.bindUsername,
        password: values.bindPassword,
      });

      if (!result) {
        Toast.error('注册绑定失败，请稍后重试');
        return;
      }

      if (result.code === '0000' && result.data) {
        setLoginStateByUserId(result.data);
        Toast.success('注册并绑定成功');
        navigate('/dashboard');
      } else {
        Toast.error(result.info || '注册绑定失败，请重试');
      }
    } catch (error) {
      console.error('微信注册绑定异常:', error);
      Toast.error('注册绑定失败，请稍后重试');
    } finally {
      setWechatLoading(false);
    }
  };

  return (
    <LoginContainer>
      <LoginWrapper>
        <BrandSection>
          <BrandLogo>
            <IconSafe />
          </BrandLogo>
          <BrandTitle heading={1}>
            AI Agent Station
          </BrandTitle>
          <BrandDescription>
            智能代理管理平台，为您提供专业的AI代理配置和管理服务。
            <br />
            简单易用，功能强大，助力您的业务智能化升级。
          </BrandDescription>
        </BrandSection>
        
        <LoginCard>
          {!isScanMode && (
            <>
              <LoginHeader>
                <LoginTitle heading={3}>
                  欢迎登录
                </LoginTitle>
                <LoginSubtitle>
                  请输入您的账号和密码
                </LoginSubtitle>
              </LoginHeader>

              <StyledForm onSubmit={handleLogin}>
                <Form.Input
                  field="username"
                  placeholder="请输入账号"
                  prefix={<IconUser />}
                  size="large"
                  rules={[
                    { required: true, message: '请输入账号' },
                    { min: 3, message: '账号至少3个字符' }
                  ]}
                />
                <Form.Input
                  field="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="请输入密码"
                  prefix={<IconLock />}
                  size="large"
                  suffix={
                    <Button
                      theme="borderless"
                      icon={showPassword ? <IconEyeClosed /> : <IconEyeOpened />}
                      onClick={() => setShowPassword(!showPassword)}
                      style={{ padding: '4px' }}
                    />
                  }
                  rules={[
                    { required: true, message: '请输入密码' },
                    { min: 6, message: '密码至少6个字符' }
                  ]}
                />

                <LoginButton
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                >
                  立即登录
                </LoginButton>
              </StyledForm>

              <Divider>
                <span>或</span>
              </Divider>

              <QuickLoginButton
                onClick={handleTestLogin}
                loading={loading}
              >
                使用admin账号登录
              </QuickLoginButton>

              <WechatSection>
                <WechatHeader>
                  <WechatTitle>其他登录方式</WechatTitle>
                  <WechatHint>你也可以使用微信扫码快速进入</WechatHint>
                </WechatHeader>

                <QuickLoginButton
                  onClick={handleStartWechatLogin}
                  loading={wechatLoading}
                >
                  微信扫码登录
                </QuickLoginButton>
              </WechatSection>
            </>
          )}

          {isScanMode && (
            <>
              <LoginHeader>
                <LoginTitle heading={3}>
                  微信扫码登录
                </LoginTitle>
                <LoginSubtitle>
                  微信扫码，安全登录
                </LoginSubtitle>
              </LoginHeader>

              <QrWrapper>
                <QrImage>
                  {wechatLoading || !wechatQrUrl ? (
                    <Text type="tertiary">二维码加载中...</Text>
                  ) : (
                    <img src={wechatQrUrl} alt="微信登录二维码" />
                  )}
                </QrImage>
                <QrDesc>
                  <Text strong>请使用微信「扫一扫」</Text>
                  <Text type="tertiary" style={{ fontSize: 12 }}>
                    打开微信 &gt; 扫一扫，在手机上确认即可完成登录。
                  </Text>
                  <Text type="tertiary" style={{ fontSize: 11, opacity: 0.7 }}>
                    为了安全考虑，二维码会在一段时间后失效，如无法登录请点击重新扫码。
                  </Text>
                  <Button
                    theme="borderless"
                    onClick={handleGuestLogin}
                    style={{ paddingLeft: 0, marginTop: theme.spacing.sm }}
                  >
                    暂不扫码，先以游客身份进入
                  </Button>
                </QrDesc>
              </QrWrapper>

              {wechatStep === 'binding' && (
                <BindFormWrapper>
                  <Text strong style={{ display: 'block', marginBottom: theme.spacing.sm }}>
                    绑定账号
                  </Text>
                  <Text type="tertiary" style={{ fontSize: 12, marginBottom: theme.spacing.md, display: 'block' }}>
                    检测到当前微信尚未绑定账号，请设置用户名和密码完成注册绑定。
                  </Text>
                  <Form onSubmit={handleBindRegister}>
                    <Form.Input
                      field="bindUsername"
                      placeholder="请输入要绑定的用户名"
                      prefix={<IconUser />}
                      rules={[{ required: true, message: '请输入用户名' }]}
                    />
                    <Form.Input
                      field="bindPassword"
                      type="password"
                      placeholder="请设置登录密码"
                      prefix={<IconLock />}
                      rules={[
                        { required: true, message: '请输入密码' },
                        { min: 6, message: '密码至少6个字符' },
                      ]}
                    />
                    <LoginButton
                      type="primary"
                      htmlType="submit"
                      loading={wechatLoading}
                      style={{ marginTop: theme.spacing.sm }}
                    >
                      注册并绑定
                    </LoginButton>
                  </Form>
                </BindFormWrapper>
              )}

              <Button
                theme="borderless"
                onClick={() => {
                  setIsScanMode(false);
                  setWechatStep('idle');
                  clearPolling();
                }}
                style={{
                  marginTop: theme.spacing.lg,
                  paddingLeft: 0,
                  fontSize: 12,
                  color: theme.colors.primary,
                }}
              >
                返回账号登录
              </Button>
            </>
          )}
        </LoginCard>
      </LoginWrapper>
    </LoginContainer>
  );
};
export default LoginPage;
