const { SecretsManagerClient, GetSecretValueCommand } = require("@aws-sdk/client-secrets-manager");

const secrets = new SecretsManagerClient({});

async function resolveJobToken() {
  const secretArn = process.env.BUDGETBOT_SECRET_ARN;
  if (!secretArn) {
    throw new Error("Missing BUDGETBOT_SECRET_ARN");
  }

  const resp = await secrets.send(new GetSecretValueCommand({ SecretId: secretArn }));
  if (!resp.SecretString) {
    throw new Error("SecretString is empty");
  }

  const data = JSON.parse(resp.SecretString);
  return data.BUDGETBOT_JOB_TOKEN;
}

exports.handler = async () => {
  const baseUrl = process.env.BUDGETBOT_BASE_URL;

  if (!baseUrl) {
    console.error("Missing BUDGETBOT_BASE_URL");
    return { statusCode: 500, body: "Missing config" };
  }

  const token = await resolveJobToken();
  if (!token) {
    console.error("Missing BUDGETBOT_JOB_TOKEN in secret");
    return { statusCode: 500, body: "Missing job token" };
  }

  const response = await fetch(`${baseUrl}/jobs/daily`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Budgetbot-Job-Token": token
    },
    body: "{}"
  });

  const text = await response.text();
  console.log("daily status", response.status);
  console.log("daily body", text);

  return { statusCode: response.status, body: text };
};
