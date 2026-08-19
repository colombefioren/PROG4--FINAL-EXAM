document.querySelectorAll('.graduate-download').forEach((button) => {
  button.addEventListener('click', () =>
    downloadGraduateList(button.dataset.promotionId),
  );
});

async function downloadGraduateList(promotionId) {
  try {
    const response = await fetch(
      `/promotions/${promotionId}/graduates/export`,
    );
    if (!response.ok) {
      throw new Error(`Download failed with status ${response.status}`);
    }
    const { url } = await response.json();
    const link = document.createElement('a');
    link.href = url;
    document.body.appendChild(link);
    link.click();
    link.remove();
  } catch (error) {
    alert(error.message);
  }
}