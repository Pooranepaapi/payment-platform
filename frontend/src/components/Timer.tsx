import { useState, useEffect, useCallback } from 'react';
import type { TimerProps } from '../types';

export default function Timer({ expiresAt, onExpire, className = '' }: TimerProps) {
  const calculateTimeLeft = useCallback(() => {
    const expiryTime = new Date(expiresAt).getTime();
    const now = Date.now();
    const difference = expiryTime - now;

    if (difference <= 0) {
      return { minutes: 0, seconds: 0, expired: true };
    }

    const minutes = Math.floor((difference / 1000 / 60) % 60);
    const seconds = Math.floor((difference / 1000) % 60);

    return { minutes, seconds, expired: false };
  }, [expiresAt]);

  const [timeLeft, setTimeLeft] = useState(calculateTimeLeft);

  useEffect(() => {
    // Immediately calculate time left
    const newTimeLeft = calculateTimeLeft();
    setTimeLeft(newTimeLeft);

    if (newTimeLeft.expired) {
      onExpire();
      return;
    }

    const timer = setInterval(() => {
      const updated = calculateTimeLeft();
      setTimeLeft(updated);

      if (updated.expired) {
        clearInterval(timer);
        onExpire();
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [calculateTimeLeft, onExpire]);

  const formatTime = (value: number) => value.toString().padStart(2, '0');

  // Determine color based on time remaining
  const getTimerColor = () => {
    const totalSeconds = timeLeft.minutes * 60 + timeLeft.seconds;
    if (totalSeconds <= 60) return 'text-red-600 bg-red-50';
    if (totalSeconds <= 180) return 'text-orange-600 bg-orange-50';
    return 'text-gray-700 bg-gray-100';
  };

  if (timeLeft.expired) {
    return (
      <div
        className={`inline-flex items-center px-3 py-1.5 rounded-full text-red-600 bg-red-50 font-medium ${className}`}
      >
        <svg
          className="w-4 h-4 mr-1.5"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
        Expired
      </div>
    );
  }

  return (
    <div
      className={`inline-flex items-center px-3 py-1.5 rounded-full font-mono font-medium ${getTimerColor()} ${className}`}
    >
      <svg
        className="w-4 h-4 mr-1.5"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
        />
      </svg>
      {formatTime(timeLeft.minutes)}:{formatTime(timeLeft.seconds)}
    </div>
  );
}
