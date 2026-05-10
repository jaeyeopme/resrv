module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      [
        'feat',
        'fix',
        'docs',
        'test',
        'refactor',
        'build',
        'chore',
        'ci',
        'perf',
        'style',
        'revert',
      ],
    ],
    'scope-case': [2, 'always', 'kebab-case'],
    'header-max-length': [2, 'always', 72],
    'body-max-line-length': [0],
    'footer-max-line-length': [0],
  },
};
