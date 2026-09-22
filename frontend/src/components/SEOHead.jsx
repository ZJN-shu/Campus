import { Helmet } from 'react-helmet-async';

/**
 * SEO 头部组件：动态设置页面 Meta 标签、Open Graph、结构化数据
 */
export default function SEOHead({
  title = '校园平台 - 让校园生活更便捷',
  description = '校园综合服务平台：论坛交流、二手交易、跑腿代办，一站式解决校园生活需求',
  keywords = '校园,论坛,二手,跑腿,交易,代取快递',
  ogType = 'website',
  ogImage = '/og-default.png',
  canonical,
  jsonLd,
}) {
  const fullTitle = title.includes('校园平台') ? title : `${title} - 校园平台`;
  const url = canonical || (typeof window !== 'undefined' ? window.location.href : '');

  return (
    <Helmet>
      <title>{fullTitle}</title>
      <meta name="description" content={description} />
      <meta name="keywords" content={keywords} />
      <link rel="canonical" href={url} />

      {/* Open Graph */}
      <meta property="og:title" content={fullTitle} />
      <meta property="og:description" content={description} />
      <meta property="og:type" content={ogType} />
      <meta property="og:url" content={url} />
      <meta property="og:image" content={ogImage} />
      <meta property="og:site_name" content="校园平台" />

      {/* Twitter Card */}
      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:title" content={fullTitle} />
      <meta name="twitter:description" content={description} />

      {/* 结构化数据 JSON-LD */}
      {jsonLd && (
        <script type="application/ld+json">{JSON.stringify(jsonLd)}</script>
      )}
    </Helmet>
  );
}
