'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { register, sendEmailVerification, verifyEmailCode, checkNickname, checkEmail } from '../../api/auth';

export default function SignupPage() {
  const [nickname, setNickname] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [showVerification, setShowVerification] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [nicknameChecked, setNicknameChecked] = useState(false);
  const [nicknameAvailable, setNicknameAvailable] = useState(false);
  const [checkingNickname, setCheckingNickname] = useState(false);
  const [nicknameError, setNicknameError] = useState('');
  const [emailChecked, setEmailChecked] = useState(false);
  const [emailAvailable, setEmailAvailable] = useState(false);
  const [checkingEmail, setCheckingEmail] = useState(false);
  const [emailError, setEmailError] = useState('');
  const [emailVerified, setEmailVerified] = useState(false);
  const [passwordError, setPasswordError] = useState('');
  const [confirmPasswordError, setConfirmPasswordError] = useState('');
  const router = useRouter();

  // 비밀번호 유효성 검사
  const validatePassword = (pwd) => {
    if (!pwd) {
      return '비밀번호를 입력해주세요';
    }
    if (pwd.length < 8 || pwd.length > 50) {
      return '비밀번호는 8~50 자여야 합니다.';
    }
    // 영문과 특수문자(@$!%*#?&)를 최소 1개씩 포함해야 함
    const passwordPattern = /^(?=.*[A-Za-z])(?=.*[@$!%*#?&]).+$/;
    if (!passwordPattern.test(pwd)) {
      return '비밀번호는 영문과 특수문자를 최소 1개씩 포함해야 합니다.';
    }
    return '';
  };

  // 비밀번호 확인 검사
  const validateConfirmPassword = (pwd, confirmPwd) => {
    if (!confirmPwd) {
      return '비밀번호 확인을 입력해주세요';
    }
    if (pwd !== confirmPwd) {
      return '비밀번호가 일치하지 않습니다.';
    }
    return '';
  };

  const handleCheckNickname = async () => {
    if (!nickname || nickname.length < 2) {
      setNicknameError('닉네임은 2자 이상 입력해주세요.');
      setNicknameChecked(false);
      return;
    }

    setNicknameError('');
    setCheckingNickname(true);

    try {
      // 백엔드 응답: true면 중복(사용 불가), false면 사용 가능
      const isDuplicate = await checkNickname(nickname);
      setNicknameChecked(true);
      setNicknameAvailable(!isDuplicate);
      
      if (isDuplicate) {
        setNicknameError('이미 사용 중인 닉네임입니다.');
      }
    } catch (err) {
      setNicknameError(err.message || '닉네임 확인에 실패했습니다.');
      setNicknameChecked(false);
      setNicknameAvailable(false);
    } finally {
      setCheckingNickname(false);
    }
  };

  const handleEmailVerification = async () => {
    if (!email) {
      setEmailError('이메일을 입력해주세요.');
      setEmailChecked(false);
      return;
    }

    // 이메일 형식 검증
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setEmailError('올바른 이메일 형식을 입력해주세요.');
      setEmailChecked(false);
      return;
    }

    setEmailError('');
    setError('');
    setCheckingEmail(true);

    try {
      // 1단계: 이메일 중복 확인
      const isDuplicate = await checkEmail(email);
      setEmailChecked(true);
      setEmailAvailable(!isDuplicate);
      
      if (isDuplicate) {
        // 중복이면 에러 표시하고 종료
        setEmailError('이미 사용 중인 이메일입니다.');
        return;
      }

      // 2단계: 중복이 없으면 이메일 인증 코드 발송
      setCheckingEmail(false);
      setVerifying(true);
      setEmailVerified(false); // 인증 코드 재발송 시 인증 상태 초기화
      
      await sendEmailVerification(email);
      setShowVerification(true);
    } catch (err) {
      // 중복 확인 실패 또는 인증 코드 발송 실패
      if (!emailChecked) {
        setEmailError(err.message || '이메일 확인에 실패했습니다.');
        setEmailChecked(false);
        setEmailAvailable(false);
      } else {
      setError(err.message || '인증 코드 전송에 실패했습니다.');
      }
    } finally {
      setCheckingEmail(false);
      setVerifying(false);
    }
  };

  const handleVerifyCode = async () => {
    if (!verificationCode) {
      setError('인증번호를 입력해주세요.');
      return;
    }

    setError('');
    setVerifying(true);

    try {
      // 인증 코드 확인 API 호출
      await verifyEmailCode(email, verificationCode);
      setEmailVerified(true);
      setError('');
    } catch (err) {
      setError(err.message || '인증번호가 올바르지 않습니다.');
      setEmailVerified(false);
    } finally {
      setVerifying(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // 닉네임 중복 확인 체크
    if (!nicknameChecked || !nicknameAvailable) {
      setError('닉네임 중복 확인을 완료해주세요.');
      return;
    }

    // 비밀번호 유효성 검사
    const passwordValidationError = validatePassword(password);
    if (passwordValidationError) {
      setPasswordError(passwordValidationError);
      setError(passwordValidationError);
      return;
    }

    // 비밀번호 확인 검사
    const confirmPasswordValidationError = validateConfirmPassword(password, confirmPassword);
    if (confirmPasswordValidationError) {
      setConfirmPasswordError(confirmPasswordValidationError);
      setError(confirmPasswordValidationError);
      return;
    }

    if (!showVerification || !emailVerified) {
      setError('이메일 인증을 완료해주세요.');
      return;
    }

    setLoading(true);

    try {
      await register({
        nickname,
        email,
        password,
        authCode: verificationCode,
      });

      // 회원가입 성공 후 로그인 페이지로 리다이렉트
      router.push('/auth/login');
    } catch (err) {
      // 백엔드 validation 에러 처리
      let errorMsg = err.message || '회원가입에 실패했습니다.';
      
      // Spring Boot validation 에러 형식 처리
      if (err.data && err.data.errors && Array.isArray(err.data.errors)) {
        const validationErrors = err.data.errors;
        // 비밀번호 관련 에러 찾기
        const passwordError = validationErrors.find(e => e.field === 'password');
        if (passwordError) {
          errorMsg = passwordError.message || '비밀번호 형식이 올바르지 않습니다.';
          setPasswordError(passwordError.message || '비밀번호 형식이 올바르지 않습니다.');
        }
        // 닉네임 관련 에러 찾기
        const nicknameError = validationErrors.find(e => e.field === 'nickname');
        if (nicknameError) {
          errorMsg = nicknameError.message || '닉네임 형식이 올바르지 않습니다.';
          setNicknameError(nicknameError.message || '닉네임 형식이 올바르지 않습니다.');
        }
        // 이메일 관련 에러 찾기
        const emailError = validationErrors.find(e => e.field === 'email');
        if (emailError) {
          errorMsg = emailError.message || '이메일 형식이 올바르지 않습니다.';
          setEmailError(emailError.message || '이메일 형식이 올바르지 않습니다.');
        }
      }
      
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4 py-8">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-lg shadow-md p-8">
          {/* 헤더 */}
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">
              HomeDataZip
            </h1>
            <p className="text-sm text-gray-500">
              부동산 실거래 분석 플랫폼
            </p>
          </div>

          {/* 회원가입 폼 */}
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* 닉네임 입력 */}
            <div>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg
                    className="h-5 w-5 text-gray-400"
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 20 20"
                    fill="currentColor"
                  >
                    <path
                      fillRule="evenodd"
                      d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"
                      clipRule="evenodd"
                    />
                  </svg>
                </div>
                <input
                  type="text"
                  value={nickname}
                  onChange={(e) => {
                    setNickname(e.target.value);
                    setNicknameChecked(false);
                    setNicknameAvailable(false);
                    setNicknameError('');
                  }}
                  placeholder="닉네임"
                  required
                  className={`block w-full pl-10 pr-24 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600 ${
                    nicknameChecked && nicknameAvailable
                      ? 'border-green-500'
                      : nicknameChecked && !nicknameAvailable
                      ? 'border-red-500'
                      : 'border-gray-300'
                  }`}
                />
                <button
                  type="button"
                  onClick={handleCheckNickname}
                  disabled={checkingNickname || !nickname || nickname.length < 2}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center"
                >
                  <span className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors disabled:bg-gray-50 disabled:text-gray-400 disabled:cursor-not-allowed text-sm font-medium">
                    {checkingNickname ? '확인중...' : '중복확인'}
                  </span>
                </button>
              </div>
              {/* 닉네임 체크 결과 메시지 */}
              {nicknameChecked && nicknameAvailable && (
                <p className="mt-1 text-sm text-green-600">사용 가능한 닉네임입니다.</p>
              )}
              {nicknameError && (
                <p className="mt-1 text-sm text-red-500">{nicknameError}</p>
              )}
            </div>

            {/* 이메일 입력 */}
            <div>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg
                    className="h-5 w-5 text-gray-400"
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 20 20"
                    fill="currentColor"
                  >
                    <path d="M2.003 5.884L10 9.882l7.997-3.998A2 2 0 0016 4H4a2 2 0 00-1.997 1.884z" />
                    <path d="M18 8.118l-8 4-8-4V14a2 2 0 002 2h12a2 2 0 002-2V8.118z" />
                  </svg>
                </div>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    setEmailChecked(false);
                    setEmailAvailable(false);
                    setEmailError('');
                    setShowVerification(false);
                    setVerificationCode('');
                    setEmailVerified(false);
                  }}
                  placeholder="이메일"
                  required
                  className={`block w-full pl-10 pr-24 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600 ${
                    emailChecked && emailAvailable
                      ? 'border-green-500'
                      : emailChecked && !emailAvailable
                      ? 'border-red-500'
                      : 'border-gray-300'
                  }`}
                />
                <button
                  type="button"
                  onClick={handleEmailVerification}
                  disabled={checkingEmail || verifying || !email}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center"
                >
                  <span className="px-4 py-2 bg-blue-100 text-blue-700 rounded-lg hover:bg-blue-200 transition-colors disabled:bg-gray-50 disabled:text-gray-400 disabled:cursor-not-allowed text-sm font-medium">
                    {checkingEmail ? '확인중...' : verifying ? '전송중...' : '인증'}
                  </span>
                </button>
              </div>
              {/* 이메일 체크 결과 메시지 */}
              {emailChecked && emailAvailable && showVerification && (
                <p className="mt-1 text-sm text-green-600">인증 코드가 발송되었습니다.</p>
              )}
              {emailError && (
                <p className="mt-1 text-sm text-red-500">{emailError}</p>
              )}
            </div>

            {/* 이메일 인증 섹션 */}
            {showVerification && (
              <div className="space-y-3">
                <label className="block text-sm font-medium text-gray-700">
                  이메일 인증
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={verificationCode}
                    onChange={(e) => {
                      setVerificationCode(e.target.value);
                      setEmailVerified(false);
                      setError('');
                    }}
                    placeholder="인증번호 입력"
                    disabled={emailVerified}
                    className={`flex-1 px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600 ${
                      emailVerified
                        ? 'border-green-500 bg-green-50'
                        : 'border-gray-300'
                    }`}
                  />
                  <button
                    type="button"
                    onClick={handleVerifyCode}
                    disabled={verifying || !verificationCode || emailVerified}
                    className="px-6 py-3 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors disabled:bg-gray-50 disabled:text-gray-400 disabled:cursor-not-allowed font-medium"
                  >
                    {verifying ? '확인중...' : emailVerified ? '인증완료' : '확인'}
                  </button>
                </div>
                {emailVerified && (
                  <p className="text-sm text-green-600 font-medium">
                    ✓ 이메일 인증이 완료되었습니다.
                  </p>
                )}
              </div>
            )}

            {/* 비밀번호 입력 */}
            <div>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg
                    className="h-5 w-5 text-gray-400"
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 20 20"
                    fill="currentColor"
                  >
                    <path
                      fillRule="evenodd"
                      d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z"
                      clipRule="evenodd"
                    />
                  </svg>
                </div>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => {
                    const newPassword = e.target.value;
                    setPassword(newPassword);
                    const error = validatePassword(newPassword);
                    setPasswordError(error);
                    // 비밀번호 확인도 다시 검증
                    if (confirmPassword) {
                      setConfirmPasswordError(validateConfirmPassword(newPassword, confirmPassword));
                    }
                    setError('');
                  }}
                  placeholder="비밀번호"
                  required
                  className={`block w-full pl-10 pr-3 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600 ${
                    passwordError ? 'border-red-500' : password && !passwordError ? 'border-green-500' : 'border-gray-300'
                  }`}
                />
              </div>
              {passwordError && (
                <p className="mt-1 text-sm text-red-500">{passwordError}</p>
              )}
            </div>

            {/* 비밀번호 확인 입력 */}
            <div>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg
                    className="h-5 w-5 text-gray-400"
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 20 20"
                    fill="currentColor"
                  >
                    <path
                      fillRule="evenodd"
                      d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z"
                      clipRule="evenodd"
                    />
                  </svg>
                </div>
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => {
                    const newConfirmPassword = e.target.value;
                    setConfirmPassword(newConfirmPassword);
                    const error = validateConfirmPassword(password, newConfirmPassword);
                    setConfirmPasswordError(error);
                    setError('');
                  }}
                  placeholder="비밀번호 확인"
                  required
                  className={`block w-full pl-10 pr-3 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600 ${
                    confirmPasswordError ? 'border-red-500' : confirmPassword && !confirmPasswordError && password === confirmPassword ? 'border-green-500' : 'border-gray-300'
                  }`}
                />
              </div>
              {confirmPasswordError && (
                <p className="mt-1 text-sm text-red-500">{confirmPasswordError}</p>
              )}
            </div>

            {/* 에러 메시지 */}
            {error && (
              <div className="text-red-500 text-sm text-center">
                {error}
              </div>
            )}

            {/* 회원가입 버튼 */}
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-blue-600 text-white font-bold py-3 rounded-lg hover:bg-blue-700 transition-colors disabled:bg-blue-400 disabled:cursor-not-allowed"
            >
              {loading ? '회원가입 중...' : '회원가입'}
            </button>
          </form>

          {/* 하단 링크 */}
          <div className="mt-6 flex justify-between text-sm">
            <Link
              href="/auth/login"
              className="text-gray-700 hover:text-gray-900 transition-colors"
            >
              로그인으로 돌아가기
            </Link>
            <Link
              href="/auth/forgot-password"
              className="text-gray-700 hover:text-gray-900 transition-colors"
            >
              비밀번호 찾기
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
