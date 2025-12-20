export interface ReaderLayoutConfig {
  leftSidebarWidth: string;
  rightSidebarWidth: string;
  mainWidth: string;
  mainHeight: string;
}

export const getSidebarWidths = (isMobile: boolean, isTablet: boolean) => {
  if (isMobile) {
    return { left: '100%', right: '100%' };
  }
  if (isTablet) {
    return { left: '260px', right: '260px' };
  }
  return { left: '320px', right: '380px' };
};

export const getMainWidth = (isMobile: boolean, isTablet: boolean) => {
  return '100%';
};

export const getMainHeight = (isMobile: boolean, isTablet: boolean) => {
  return '100%';
};
