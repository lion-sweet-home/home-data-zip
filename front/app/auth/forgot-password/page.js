'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { verifyEmailCode } from '../../api/auth';
import { findPassword, resetPassword } from '../../api/user';

const STEP = {
  EMAIL: 1,
  VERIFY: 2,
  RESET: 3,
};

export default function ForgotPasswordPage() {
  const [step, setStep] = useState(STEP.EMAIL);
  const [email, setEmail] = useState('');
  const [authCode, setAuthCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [passwordError, setPasswordError] = useState('');
  const [confirmPasswordError, setConfirmPasswordError] = useState('');
  const [success, setSuccess] = useState(false);
  const router = useRouter();

  const validatePassword = (pwd) => {
    if (!pwd) return '비밀번호를 입력해주세요.';
    if (pwd.length < 8 || pwd.length > 50) return '비밀번호는 8~50자여야 합니다.';
    const passwordPattern = /^(?=.*[A-Za-z])(?=.*[@$!%*#?&]).+$/;
    if (!passwordPattern.test(pwd)) return '비밀번호는 영문과 특수문자를 최소 1개씩 포함해야 합니다.';
    return '';
  };

  const validateConfirmPassword = (pwd, confirmPwd) => {
    if (!confirmPwd) return '비밀번호 확인을 입력해주세요.';
    if (pwd !== confirmPwd) return '비밀번호가 일치하지 않습니다.';
    return '';
  };

  const handleSendCode = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await findPassword(email);
      setStep(STEP.VERIFY);
    } catch (err) {
      setError(err.message || '등록되지 않은 이메일이거나 인증코드 발송에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyCode = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await verifyEmailCode(email, authCode);
      setStep(STEP.RESET);
    } catch (err) {
      setError(err.message || '인증번호가 올바르지 않습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setError('');

    const pwdError = validatePassword(newPassword);
    if (pwdError) {
      setPasswordError(pwdError);
      setError(pwdError);
      return;
    }

    const confirmError = validateConfirmPassword(newPassword, confirmPassword);
    if (confirmError) {
      setConfirmPasswordError(confirmError);
      setError(confirmError);
      return;
    }

    setLoading(true);

    try {
      await resetPassword(email, newPassword, confirmPassword);
      setSuccess(true);
    } catch (err) {
      setError(err.message || '비밀번호 재설정에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="w-full max-w-md">
          <div className="bg-white rounded-lg shadow-md p-8 text-center">
            <div className="mb-6">
              <div className="mx-auto w-16 h-16 bg-green-100 rounded-full flex items-center justify-center">
                <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              </div>
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">비밀번호 재설정 완료</h2>
            <p className="text-gray-500 mb-8">새로운 비밀번호로 로그인해주세요.</p>
            <Link
              href="/auth/login"
              className="inline-block w-full bg-blue-600 text-white font-bold py-3 rounded-lg hover:bg-blue-700 transition-colors text-center"
            >
              로그인으로 이동
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const stepLabels = ['이메일 입력', '인증코드 확인', '비밀번호 재설정'];

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4 py-8">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-lg shadow-md p-8">
          {/* 헤더 */}
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">HomeDataZip</h1>
            <p className="text-sm text-gray-500">비밀번호 찾기</p>
          </div>

          {/* 단계 인디케이터 */}
          <div className="flex items-center justify-center mb-8">
            {stepLabels.map((label, index) => {
              const stepNum = index + 1;
              const isActive = step === stepNum;
              const isCompleted = step > stepNum;
              return (
                <div key={stepNum} className="flex items-center">
                  <div className="flex flex-col items-center">
                    <div
                      className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-colors ${
                        isCompleted
                          ? 'bg-green-500 text-white'
                          : isActive
                          ? 'bg-blue-600 text-white'
                          : 'bg-gray-200 text-gray-500'
                      }`}
                    >
                      {isCompleted ? (
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                      ) : (
                        stepNum
                      )}
                    </div>
                    <span className={`text-xs mt-1 ${isActive ? 'text-blue-600 font-medium' : 'text-gray-400'}`}>
                      {label}
                    </span>
                  </div>
                  {index < stepLabels.length - 1 && (
                    <div className={`w-12 h-0.5 mx-1 mb-5 ${step > stepNum ? 'bg-green-500' : 'bg-gray-200'}`} />
                  )}
                </div>
              );
            })}
          </div>

          {/* Step 1: 이메일 입력 */}
          {step === STEP.EMAIL && (
            <form onSubmit={handleSendCode} className="space-y-6">
              <p className="text-sm text-gray-600 text-center">
                가입 시 사용한 이메일을 입력해주세요.<br />
                인증코드를 발송해드립니다.
              </p>
              <div>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <svg className="h-5 w-5 text-gray-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M2.003 5.884L10 9.882l7.997-3.998A2 2 0 0016 4H4a2 2 0 00-1.997 1.884z" />
                      <path d="M18 8.118l-8 4-8-4V14a2 2 0 002 2h12a2 2 0 002-2V8.118z" />
                    </svg>
                  </div>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => { setEmail(e.target.value); setError(''); }}
                    placeholder="이메일"
                    required
                    className="block w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600"
                  />
                </div>
              </div>

              {error && (
                <div className="text-red-500 text-sm text-center">{error}</div>
              )}

              <button
                type="submit"
                disabled={loading || !email}
                className="w-full bg-blue-600 text-white font-bold py-3 rounded-lg hover:bg-blue-700 transition-colors disabled:bg-blue-400 disabled:cursor-not-allowed"
              >
                {loading ? '발송 중...' : '인증코드 발송'}
              </button>
            </form>
          )}

          {/* Step 2: 인증코드 확인 */}
          {step === STEP.VERIFY && (
            <form onSubmit={handleVerifyCode} className="space-y-6">
              <p className="text-sm text-gray-600 text-center">
                <span className="font-medium text-gray-900">{email}</span>으로<br />
                발송된 인증코드를 입력해주세요.
              </p>
              <div>
                <input
                  type="text"
                  value={authCode}
                  onChange={(e) => { setAuthCode(e.target.value); setError(''); }}
                  placeholder="인증코드 입력"
                  required
                  className="block w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600 text-center text-lg tracking-widest"
                />
              </div>

              {error && (
                <div className="text-red-500 text-sm text-center">{error}</div>
              )}

              <button
                type="submit"
                disabled={loading || !authCode}
                className="w-full bg-blue-600 text-white font-bold py-3 rounded-lg hover:bg-blue-700 transition-colors disabled:bg-blue-400 disabled:cursor-not-allowed"
              >
                {loading ? '확인 중...' : '인증코드 확인'}
              </button>

              <button
                type="button"
                onClick={async () => {
                  setError('');
                  setLoading(true);
                  try {
                    await findPassword(email);
                    setError('');
                    setAuthCode('');
                  } catch (err) {
                    setError(err.message || '인증코드 재발송에 실패했습니다.');
                  } finally {
                    setLoading(false);
                  }
                }}
                disabled={loading}
                className="w-full text-sm text-gray-500 hover:text-gray-700 transition-colors disabled:text-gray-300"
              >
                인증코드 재발송
              </button>
            </form>
          )}

          {/* Step 3: 비밀번호 재설정 */}
          {step === STEP.RESET && (
            <form onSubmit={handleResetPassword} className="space-y-6">
              <p className="text-sm text-gray-600 text-center">
                새로운 비밀번호를 입력해주세요.
              </p>

              {/* 새 비밀번호 */}
              <div>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <svg className="h-5 w-5 text-gray-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
                    </svg>
                  </div>
                  <input
                    type="password"
                    value={newPassword}
                    onChange={(e) => {
                      const pwd = e.target.value;
                      setNewPassword(pwd);
                      setPasswordError(validatePassword(pwd));
                      if (confirmPassword) {
                        setConfirmPasswordError(validateConfirmPassword(pwd, confirmPassword));
                      }
                      setError('');
                    }}
                    placeholder="새 비밀번호"
                    required
                    className={`block w-full pl-10 pr-3 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600 ${
                      passwordError ? 'border-red-500' : newPassword && !passwordError ? 'border-green-500' : 'border-gray-300'
                    }`}
                  />
                </div>
                {passwordError && (
                  <p className="mt-1 text-sm text-red-500">{passwordError}</p>
                )}
              </div>

              {/* 비밀번호 확인 */}
              <div>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <svg className="h-5 w-5 text-gray-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
                    </svg>
                  </div>
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => {
                      const pwd = e.target.value;
                      setConfirmPassword(pwd);
                      setConfirmPasswordError(validateConfirmPassword(newPassword, pwd));
                      setError('');
                    }}
                    placeholder="새 비밀번호 확인"
                    required
                    className={`block w-full pl-10 pr-3 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600 ${
                      confirmPasswordError ? 'border-red-500' : confirmPassword && !confirmPasswordError ? 'border-green-500' : 'border-gray-300'
                    }`}
                  />
                </div>
                {confirmPasswordError && (
                  <p className="mt-1 text-sm text-red-500">{confirmPasswordError}</p>
                )}
              </div>

              {error && (
                <div className="text-red-500 text-sm text-center">{error}</div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-blue-600 text-white font-bold py-3 rounded-lg hover:bg-blue-700 transition-colors disabled:bg-blue-400 disabled:cursor-not-allowed"
              >
                {loading ? '재설정 중...' : '비밀번호 재설정'}
              </button>
            </form>
          )}

          {/* 하단 링크 */}
          <div className="mt-6 flex justify-between text-sm">
            <Link href="/auth/login" className="text-gray-700 hover:text-gray-900 transition-colors">
              로그인으로 돌아가기
            </Link>
            <Link href="/auth/register" className="text-gray-700 hover:text-gray-900 transition-colors">
              회원가입
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
