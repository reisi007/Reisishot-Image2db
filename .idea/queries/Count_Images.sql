select lens, count(*) as cnt
from main.Image
where lens is not null
group by lens
having cnt >= 100
order by cnt desc , lens desc
